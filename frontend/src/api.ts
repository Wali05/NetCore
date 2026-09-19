export type IpStatus = 'AVAILABLE' | 'ALLOCATED' | 'RESERVED'
export type DeviceType = 'SERVER' | 'WORKSTATION' | 'ROUTER' | 'SWITCH' | 'OTHER'

export interface Subnet {
  id: number
  cidr: string
  networkAddress: string
  prefixLength: number
  gatewayAddress: string | null
  totalAddresses: number
  availableAddresses: number
  allocatedAddresses: number
  reservedAddresses: number
  utilizationPercent: number
}

export interface Address {
  id: number
  address: string
  status: IpStatus
  networkInterfaceId: number | null
  networkInterfaceName: string | null
  deviceName: string | null
}

export interface Page<T> {
  content: T[]
  page: number
  totalPages: number
  totalElements: number
}

export interface Device {
  id: number
  name: string
  type: DeviceType
}

export interface NetworkInterface {
  id: number
  deviceId: number
  deviceName: string
  name: string
  macAddress: string
  assignedAddresses: { address: string; subnetId: number }[]
}

interface ApiError {
  message?: string
  fieldErrors?: Record<string, string>
}

export async function api<T>(path: string, options?: RequestInit): Promise<T> {
  const response = await fetch(`/api/v1${path}`, {
    ...options,
    headers: { 'Content-Type': 'application/json', ...options?.headers },
  })

  if (!response.ok) {
    const error = (await response.json().catch(() => ({}))) as ApiError
    const fieldMessage = Object.values(error.fieldErrors ?? {})[0]
    throw new Error(fieldMessage || error.message || `Request failed (${response.status})`)
  }

  return response.json() as Promise<T>
}

export function post<T>(path: string, body?: object): Promise<T> {
  return api<T>(path, { method: 'POST', body: body ? JSON.stringify(body) : undefined })
}
