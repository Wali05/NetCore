import type { Device, Subnet } from '../api'
import { SubnetTable } from './SubnetTable'

interface OverviewViewProps {
  subnets: Subnet[]
  devices: Device[]
  totals: { total: number; available: number; allocated: number; reserved: number }
  onNavigate: (view: 'subnets' | 'devices') => void
  onSelectSubnet: (id: number) => void
  onShowSubnetForm: () => void
}

export function OverviewView({
  subnets,
  devices,
  totals,
  onNavigate,
  onSelectSubnet,
  onShowSubnetForm,
}: OverviewViewProps) {
  const addressCapacity = totals.available + totals.allocated

  return (
    <>
      <section className="page-heading">
        <div>
          <div className="eyebrow">WORKSPACE OVERVIEW</div>
          <h1>Your address space</h1>
          <p>Availability and assignments across managed subnets.</p>
        </div>
      </section>
      <section className="overview-summary" aria-label="Address summary">
        <div className="summary-lead">
          <span className="meta-label">AVAILABLE TO ASSIGN</span>
          <div className="summary-value">{totals.available.toLocaleString()}</div>
          <p>
            Addresses ready across {subnets.length} {subnets.length === 1 ? 'subnet' : 'subnets'}.
          </p>
          <button className="summary-link" onClick={() => onNavigate('subnets')}>
            Open address space <span aria-hidden="true">→</span>
          </button>
        </div>
        <div className="summary-detail">
          <div className="summary-heading">
            Pool capacity <span>{addressCapacity.toLocaleString()} assignable</span>
          </div>
          <div
            className="capacity-bar"
            role="img"
            aria-label={`${totals.allocated} allocated and ${totals.available} available addresses`}
          >
            <span
              style={{
                width: `${addressCapacity ? (totals.allocated / addressCapacity) * 100 : 0}%`,
              }}
            />
          </div>
          <div className="summary-metrics">
            <div>
              <span>Allocated</span>
              <strong>{totals.allocated.toLocaleString()}</strong>
            </div>
            <div>
              <span>Reserved</span>
              <strong>{totals.reserved.toLocaleString()}</strong>
            </div>
            <div>
              <span>Devices</span>
              <strong>{devices.length.toLocaleString()}</strong>
            </div>
          </div>
        </div>
      </section>
      <section className="section-heading">
        <div>
          <h2>Managed subnets</h2>
          <p>Select a subnet to inspect its pool and assignments.</p>
        </div>
        <button className="text-button" onClick={() => onNavigate('subnets')}>
          View address space <span aria-hidden="true">→</span>
        </button>
      </section>
      <SubnetTable
        subnets={subnets.slice(0, 6)}
        onSelect={(id) => {
          onSelectSubnet(id)
          onNavigate('subnets')
        }}
      />
      <div className="quick-actions">
        <button onClick={onShowSubnetForm}>
          <span>01 / NETWORK</span>
          <strong>Create a subnet</strong>
          <small>Generate a new address pool</small>
        </button>
        <button onClick={() => onNavigate('devices')}>
          <span>02 / EQUIPMENT</span>
          <strong>Manage devices</strong>
          <small>Add interfaces for assignments</small>
        </button>
      </div>
    </>
  )
}
