import type { Subnet } from '../api'
import { CopyButton } from './CopyButton'

interface SubnetTableProps {
  subnets: Subnet[]
  onSelect: (id: number) => void
  selected?: number | null
}

export function SubnetTable({ subnets, onSelect, selected }: SubnetTableProps) {
  if (subnets.length === 0)
    return <div className="empty small">No subnets yet. Create your first pool below.</div>
  return (
    <div className="table-wrap">
      <table>
        <thead>
          <tr>
            <th>SUBNET</th>
            <th>GATEWAY</th>
            <th>USED</th>
            <th>UTILIZATION</th>
            <th></th>
          </tr>
        </thead>
        <tbody>
          {subnets.map((subnet) => (
            <tr key={subnet.id} className={selected === subnet.id ? 'selected' : ''}>
              <td>
                <span className="copyable-group">
                  <button
                    className="subnet-link mono"
                    onClick={() => onSelect(subnet.id)}
                    aria-label={`Open subnet ${subnet.cidr}`}
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
                  <div className="usage-bar">
                    <span style={{ width: `${subnet.utilizationPercent}%` }} />
                  </div>
                  <span>{Math.round(subnet.utilizationPercent)}%</span>
                </div>
              </td>
              <td className="arrow" aria-hidden="true">
                →
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}
