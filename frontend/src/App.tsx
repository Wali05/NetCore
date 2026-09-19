import { useCallback, useEffect, useMemo, useState } from 'react'
import {
  api,
  post,
  type Address,
  type Device,
  type DeviceType,
  type IpStatus,
  type NetworkInterface,
  type Page,
  type Subnet,
} from './api'
import { AddressSpaceView } from './components/AddressSpaceView'
import { DevicesView } from './components/DevicesView'
import { OverviewView } from './components/OverviewView'
import { Toast } from './components/Toast'

type View = 'overview' | 'subnets' | 'devices'
const PAGE_SIZE = 20

function App() {
  const [view, setView] = useState<View>('overview')
  const [subnets, setSubnets] = useState<Subnet[]>([])
  const [devices, setDevices] = useState<Device[]>([])
  const [selectedSubnet, setSelectedSubnet] = useState<number | null>(null)
  const [selectedDevice, setSelectedDevice] = useState<number | null>(null)
  const [allInterfaces, setAllInterfaces] = useState<NetworkInterface[]>([])
  const [addresses, setAddresses] = useState<Page<Address> | null>(null)
  const [page, setPage] = useState(0)
  const [statusFilter, setStatusFilter] = useState<'ALL' | IpStatus>('ALL')
  const [showSubnetForm, setShowSubnetForm] = useState(false)
  const [message, setMessage] = useState('')
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)
  const [loading, setLoading] = useState(true)
  const [connected, setConnected] = useState<boolean | null>(null)
  const dismissError = useCallback(() => setError(''), [])
  const dismissMessage = useCallback(() => setMessage(''), [])

  const refresh = useCallback(async () => {
    const [nextSubnets, nextDevices] = await Promise.all([
      api<Subnet[]>('/subnets'),
      api<Device[]>('/devices'),
    ])
    const nextInterfaces = (
      await Promise.all(
        nextDevices.map((device) => api<NetworkInterface[]>(`/devices/${device.id}/interfaces`)),
      )
    ).flat()
    setSubnets(nextSubnets)
    setDevices(nextDevices)
    setAllInterfaces(nextInterfaces)
    setConnected(true)
    setSelectedSubnet((current) =>
      current && nextSubnets.some((item) => item.id === current)
        ? current
        : (nextSubnets[0]?.id ?? null),
    )
    setSelectedDevice((current) =>
      current && nextDevices.some((item) => item.id === current)
        ? current
        : (nextDevices[0]?.id ?? null),
    )
  }, [])

  useEffect(() => {
    refresh()
      .catch((cause) => {
        setConnected(false)
        setError(cause.message)
      })
      .finally(() => setLoading(false))
  }, [refresh])

  useEffect(() => {
    if (selectedSubnet === null) {
      setAddresses(null)
      return
    }
    let cancelled = false
    setAddresses(null)
    const query = new URLSearchParams({ page: String(page), size: String(PAGE_SIZE) })
    if (statusFilter !== 'ALL') query.set('status', statusFilter)
    api<Page<Address>>(`/subnets/${selectedSubnet}/addresses?${query}`)
      .then((result) => {
        if (!cancelled) setAddresses(result)
      })
      .catch((cause) => {
        if (!cancelled) setError(cause.message)
      })
    return () => {
      cancelled = true
    }
  }, [selectedSubnet, page, statusFilter, subnets])

  const totals = useMemo(
    () =>
      subnets.reduce(
        (sum, subnet) => ({
          total: sum.total + subnet.totalAddresses,
          available: sum.available + subnet.availableAddresses,
          allocated: sum.allocated + subnet.allocatedAddresses,
          reserved: sum.reserved + subnet.reservedAddresses,
        }),
        { total: 0, available: 0, allocated: 0, reserved: 0 },
      ),
    [subnets],
  )

  const deviceInterfaces = allInterfaces.filter((item) => item.deviceId === selectedDevice)

  async function perform<T>(
    action: () => Promise<T>,
    success: string | ((result: T) => string),
  ): Promise<boolean> {
    setBusy(true)
    setError('')
    setMessage('')
    try {
      const result = await action()
      setPage(0)
      setMessage(typeof success === 'string' ? success : success(result))
      try {
        await refresh()
      } catch {
        setConnected(false)
        setMessage('')
        setError('The change was saved, but the dashboard could not refresh. Retry the connection.')
      }
      return true
    } catch (cause) {
      if (cause instanceof TypeError) setConnected(false)
      setError(cause instanceof Error ? cause.message : 'Something went wrong')
      return false
    } finally {
      setBusy(false)
    }
  }

  function handleCreateSubnet(network: string, prefix: number, gateway: string) {
    return perform(async () => {
      const created = await post<Subnet>('/subnets', {
        networkAddress: network,
        prefixLength: prefix,
        gatewayAddress: gateway || null,
      })
      setSelectedSubnet(created.id)
    }, 'Subnet and address pool created.')
  }

  function handleCreateDevice(name: string, type: DeviceType) {
    return perform(async () => {
      const created = await post<Device>('/devices', { name, type })
      setSelectedDevice(created.id)
    }, 'Device created.')
  }

  function handleCreateInterface(deviceId: number, name: string, macAddress: string) {
    return perform(async () => {
      await post(`/devices/${deviceId}/interfaces`, { name, macAddress })
    }, 'Interface added.')
  }

  function handleAllocate(interfaceId: string, requestedAddress: string) {
    if (selectedSubnet === null || !interfaceId) return Promise.resolve(false)
    const path = requestedAddress
      ? `/subnets/${selectedSubnet}/addresses/${encodeURIComponent(requestedAddress)}/allocate`
      : `/subnets/${selectedSubnet}/allocate-next`
    return perform(
      async () => {
        const assigned = await post<Address>(path, {
          networkInterfaceId: Number(interfaceId),
        })
        setStatusFilter('ALL')
        return assigned
      },
      (assigned) => `${assigned.address} assigned.`,
    )
  }

  function handleRelease(address: string) {
    if (selectedSubnet === null) return
    void perform(async () => {
      await post(`/subnets/${selectedSubnet}/addresses/${encodeURIComponent(address)}/release`)
      setStatusFilter('ALL')
    }, `${address} released.`)
  }

  return (
    <div className="shell">
      <aside className="sidebar">
        <div className="brand">
          <img className="brand-logo" src="/netcore-wordmark.png" alt="NetCore" />
        </div>
        <div className="nav-label">WORKSPACE</div>
        <nav aria-label="Main navigation">
          {(
            [
              ['overview', 'Overview', '01'],
              ['subnets', 'Address space', '02'],
              ['devices', 'Devices', '03'],
            ] as const
          ).map(([id, label, icon]) => (
            <button
              key={id}
              className={`nav-item ${view === id ? 'active' : ''}`}
              onClick={() => setView(id)}
              aria-current={view === id ? 'page' : undefined}
            >
              <span className="nav-icon">{icon}</span>
              {label}
            </button>
          ))}
        </nav>
        <div className="sidebar-foot">
          <span className="sidebar-foot-title">NetCore v1</span>
          <small>Local IPAM workspace</small>
        </div>
      </aside>

      <main className="main">
        <header className="topbar">
          <span className="breadcrumb">
            NetCore <span>/</span>{' '}
            {view === 'subnets' ? 'Address space' : view === 'devices' ? 'Devices' : 'Overview'}
          </span>
          <span
            className={`connection ${connected === false ? 'offline' : connected === null ? 'connecting' : ''}`}
          >
            <span className="live-dot" />{' '}
            {connected === null ? 'Connecting…' : connected ? 'API online' : 'API unavailable'}
            {connected === false && (
              <button
                className="retry"
                onClick={() => {
                  setLoading(true)
                  setConnected(null)
                  setError('')
                  refresh()
                    .catch((cause) => {
                      setConnected(false)
                      setError(cause.message)
                    })
                    .finally(() => setLoading(false))
                }}
              >
                Retry
              </button>
            )}
          </span>
        </header>

        {error && <Toast message={error} type="error" onDismiss={dismissError} />}
        {message && <Toast message={message} type="success" onDismiss={dismissMessage} />}

        {loading ? (
          <div className="empty">Loading NetCore…</div>
        ) : (
          <>
            {view === 'overview' && (
              <OverviewView
                subnets={subnets}
                devices={devices}
                totals={totals}
                onNavigate={setView}
                onSelectSubnet={setSelectedSubnet}
                onShowSubnetForm={() => {
                  setView('subnets')
                  setShowSubnetForm(true)
                }}
              />
            )}

            {view === 'subnets' && (
              <AddressSpaceView
                subnets={subnets}
                selectedSubnet={selectedSubnet}
                addresses={addresses}
                allInterfaces={allInterfaces}
                page={page}
                statusFilter={statusFilter}
                busy={busy}
                showSubnetForm={showSubnetForm}
                onSelectSubnet={setSelectedSubnet}
                onSetPage={setPage}
                onSetStatusFilter={setStatusFilter}
                onSetShowSubnetForm={setShowSubnetForm}
                onCreateSubnet={handleCreateSubnet}
                onAllocate={handleAllocate}
                onRelease={handleRelease}
                onNavigate={setView}
              />
            )}

            {view === 'devices' && (
              <DevicesView
                devices={devices}
                selectedDevice={selectedDevice}
                interfaces={deviceInterfaces}
                busy={busy}
                onSelectDevice={setSelectedDevice}
                onCreateDevice={handleCreateDevice}
                onCreateInterface={handleCreateInterface}
              />
            )}
          </>
        )}
      </main>
    </div>
  )
}

export default App
