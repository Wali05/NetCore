import type { Subnet } from '../api'
import { useState } from 'react'
import { CopyButton } from './CopyButton'
import { Icon } from './Icon'

interface SubnetTableProps {
  subnets: Subnet[]
  onSelect: (id: number) => void
  selected?: number | null
  searchable?: boolean
}

export function SubnetTable({ subnets, onSelect, selected, searchable = false }: SubnetTableProps) {
  const [search, setSearch] = useState('')
  const [sort, setSort] = useState<'network' | 'capacity' | 'available'>('network')
  const visibleSubnets = subnets
    .filter((subnet) => `${subnet.cidr} ${subnet.gatewayAddress ?? ''}`.includes(search.trim()))
    .sort((a, b) =>
      sort === 'capacity'
        ? b.utilizationPercent - a.utilizationPercent
        : sort === 'available'
          ? a.availableAddresses - b.availableAddresses
          : a.networkAddress.localeCompare(b.networkAddress, undefined, { numeric: true }) ||
            a.prefixLength - b.prefixLength,
    )
  if (subnets.length === 0)
    return (
      <div className="empty small">
        No subnets yet. Choose New subnet to create your first pool.
      </div>
    )
  return (
    <div className="subnet-inventory">
      {searchable && (
        <div className="inventory-toolbar">
          <label className="search-field">
            <Icon name="search" size={16} />
            <input
              aria-label="Search subnets"
              placeholder="Search by subnet or gateway…"
              value={search}
              onChange={(event) => setSearch(event.target.value)}
            />
            {search && (
              <button type="button" aria-label="Clear subnet search" onClick={() => setSearch('')}>
                ×
              </button>
            )}
          </label>
          <label className="sort-field">
            <span>Sort by</span>
            <select
              aria-label="Sort subnets"
              value={sort}
              onChange={(event) => setSort(event.target.value as typeof sort)}
            >
              <option value="network">Network address</option>
              <option value="capacity">Highest utilization</option>
              <option value="available">Fewest available</option>
            </select>
          </label>
        </div>
      )}
      <div className="table-wrap">
        <table>
          <thead>
            <tr>
              <th>SUBNET</th>
              <th>GATEWAY</th>
              <th>USED</th>
              <th>UTILIZATION</th>
              <th>
                <span className="sr-only">Open subnet</span>
              </th>
            </tr>
          </thead>
          <tbody>
            {visibleSubnets.map((subnet) => (
              <tr key={subnet.id} className={selected === subnet.id ? 'selected' : ''}>
                <td>
                  <span className="copyable-group">
                    <button
                      className="subnet-link mono"
                      onClick={() => onSelect(subnet.id)}
                      aria-label={`Open subnet ${subnet.cidr}`}
                      aria-current={selected === subnet.id ? 'true' : undefined}
                    >
                      {subnet.cidr}
                    </button>
                    <CopyButton text={subnet.cidr} />
                  </span>
                </td>
                <td>
                  {subnet.gatewayAddress ? (
                    <span className="copyable-group">
                      <span className="mono muted">{subnet.gatewayAddress}</span>
                      <CopyButton text={subnet.gatewayAddress} />
                    </span>
                  ) : (
                    <span className="muted">—</span>
                  )}
                </td>
                <td>
                  {subnet.allocatedAddresses}
                  <span className="muted">
                    {' '}
                    / {subnet.availableAddresses + subnet.allocatedAddresses}
                  </span>
                </td>
                <td>
                  <div className="usage">
                    <div className={`usage-bar ${subnet.utilizationPercent >= 80 ? 'high' : ''}`}>
                      <span style={{ width: `${subnet.utilizationPercent}%` }} />
                    </div>
                    <span>{Math.round(subnet.utilizationPercent)}%</span>
                  </div>
                </td>
                <td className="arrow">
                  <button
                    className="table-open"
                    aria-label={`Inspect ${subnet.cidr}`}
                    onClick={() => onSelect(subnet.id)}
                  >
                    <Icon name="arrow" size={16} />
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
        {visibleSubnets.length === 0 && (
          <div className="empty small">No subnets match “{search}”.</div>
        )}
      </div>
      {searchable && (
        <div className="inventory-result" role="status">
          {visibleSubnets.length} of {subnets.length} subnets
        </div>
      )}
    </div>
  )
}
