import type { Subnet } from './api'

function csvCell(value: string | number) {
  if (typeof value === 'number') return String(value)
  const safe = /^[=+\-@\t\r]/.test(value) ? `'${value}` : value
  return /[",\r\n]/.test(safe) ? `"${safe.replaceAll('"', '""')}"` : safe
}

export function exportSubnets(subnets: Subnet[]) {
  const rows = [
    ['Subnet', 'Gateway', 'Total', 'Available', 'Allocated', 'Reserved'],
    ...subnets.map((subnet) => [
      subnet.cidr,
      subnet.gatewayAddress ?? '',
      subnet.totalAddresses,
      subnet.availableAddresses,
      subnet.allocatedAddresses,
      subnet.reservedAddresses,
    ]),
  ]
  const csv = rows.map((row) => row.map(csvCell).join(',')).join('\r\n')
  const url = URL.createObjectURL(new Blob([csv], { type: 'text/csv;charset=utf-8' }))
  const link = document.createElement('a')
  link.href = url
  link.download = 'netcore-subnets.csv'
  link.click()
  setTimeout(() => URL.revokeObjectURL(url), 1000)
}
