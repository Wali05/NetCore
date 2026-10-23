import type { CSSProperties } from 'react'
import type { Device, NetworkInterface, Subnet } from '../api'
import { Icon } from './Icon'
import { SubnetTable } from './SubnetTable'

interface OverviewViewProps {
  subnets: Subnet[]
  devices: Device[]
  interfaces: NetworkInterface[]
  totals: { total: number; available: number; allocated: number; reserved: number }
  onNavigate: (view: 'subnets' | 'devices') => void
  onSelectSubnet: (id: number) => void
  onShowSubnetForm: () => void
}

export function OverviewView({
  subnets,
  devices,
  interfaces,
  totals,
  onNavigate,
  onSelectSubnet,
  onShowSubnetForm,
}: OverviewViewProps) {
  const usable = totals.available + totals.allocated
  const utilization = usable ? Math.round((totals.allocated / usable) * 100) : 0
  const unassigned = interfaces.filter((item) => item.assignedAddresses.length === 0).length
  const busiest = [...subnets]
    .sort((a, b) => b.utilizationPercent - a.utilizationPercent)
    .slice(0, 4)
  const deviceTypes = ['SERVER', 'ROUTER', 'SWITCH', 'WORKSTATION', 'OTHER'] as const
  const capacityReview = subnets
    .filter((subnet) => subnet.utilizationPercent >= 80)
    .sort((a, b) => b.utilizationPercent - a.utilizationPercent)

  function openSubnet(id: number) {
    onSelectSubnet(id)
    onNavigate('subnets')
  }

  return (
    <>
      <section className="page-heading">
        <div>
          <div className="eyebrow">NETWORK OPERATIONS / OVERVIEW</div>
          <h1>A clear view of your network.</h1>
          <p>Every subnet, address and assignment. One workspace.</p>
        </div>
        <button className="primary" onClick={onShowSubnetForm}>
          <Icon name="plus" size={16} /> New subnet
        </button>
      </section>

      <section className="metric-grid" aria-label="Inventory summary">
        {[
          {
            label: 'Available addresses',
            value: totals.available,
            detail: 'Ready for assignment',
            icon: 'network',
            accent: true,
          },
          {
            label: 'Allocated addresses',
            value: totals.allocated,
            detail: `${utilization}% of assignable capacity`,
            icon: 'arrow',
          },
          {
            label: 'Managed subnets',
            value: subnets.length,
            detail: `${totals.total} addresses in inventory`,
            icon: 'overview',
          },
          {
            label: 'Registered devices',
            value: devices.length,
            detail: `${interfaces.length} network interfaces`,
            icon: 'devices',
          },
        ].map((metric) => (
          <div className={`metric-card ${metric.accent ? 'accent' : ''}`} key={metric.label}>
            <div className="metric-label">
              {metric.label}
              <Icon name={metric.icon as 'network' | 'arrow' | 'overview' | 'devices'} size={17} />
            </div>
            <strong className="metric-value">{metric.value.toLocaleString()}</strong>
            <span className="metric-detail">{metric.detail}</span>
          </div>
        ))}
      </section>

      {capacityReview.length > 0 && (
        <div className="capacity-advisory">
          <span className="advisory-icon" aria-hidden="true">
            !
          </span>
          <div>
            <strong>
              {capacityReview.length} {capacityReview.length === 1 ? 'subnet is' : 'subnets are'} at
              80% capacity or above
            </strong>
            <span>
              {capacityReview[0].cidr} has {capacityReview[0].availableAddresses}{' '}
              {capacityReview[0].availableAddresses === 1 ? 'address' : 'addresses'} left to assign.
            </span>
          </div>
          <button onClick={() => openSubnet(capacityReview[0].id)}>
            Review pool <Icon name="arrow" size={16} />
          </button>
        </div>
      )}

      {subnets.length === 0 ? (
        <section className="onboarding panel">
          <div className="eyebrow">YOUR FIRST ALLOCATION</div>
          <h2>Build a small network. See how it fits.</h2>
          <p>Start with a subnet, register a device, then give its interface an address.</p>
          <div className="onboarding-steps">
            <button onClick={onShowSubnetForm}>
              <span>01</span>
              <strong>Create a subnet</strong>
              <small>Try 10.20.30.0/29</small>
              <Icon name="arrow" />
            </button>
            <button onClick={() => onNavigate('devices')}>
              <span>02</span>
              <strong>Register a device</strong>
              <small>Add an eth0 interface</small>
              <Icon name="arrow" />
            </button>
            <button onClick={() => onNavigate('subnets')}>
              <span>03</span>
              <strong>Assign an address</strong>
              <small>Choose the next available IP</small>
              <Icon name="arrow" />
            </button>
          </div>
        </section>
      ) : (
        <div className="overview-grid">
          <section className="panel capacity-panel">
            <div className="panel-heading">
              <div>
                <h2>Address capacity</h2>
                <p>Most utilized subnets · reserved addresses excluded</p>
              </div>
              <span className="subtle-tag">IPv4</span>
            </div>
            <div className="capacity-content">
              <div
                className="utilization-gauge"
                style={{ '--utilization': `${utilization}%` } as CSSProperties}
                role="img"
                aria-label={`${utilization}% of assignable addresses allocated`}
              >
                <div>
                  <strong>
                    {utilization}
                    <span>%</span>
                  </strong>
                  <small>allocated</small>
                </div>
              </div>
              <div className="capacity-subnets">
                {busiest.map((subnet) => (
                  <button
                    className="capacity-subnet"
                    key={subnet.id}
                    onClick={() => openSubnet(subnet.id)}
                  >
                    <div>
                      <strong className="mono">{subnet.cidr}</strong>
                      <span>{Math.round(subnet.utilizationPercent)}%</span>
                    </div>
                    <div className={`segment-bar ${subnet.utilizationPercent >= 80 ? 'high' : ''}`}>
                      <span style={{ width: `${subnet.utilizationPercent}%` }} />
                    </div>
                    <small>
                      {subnet.availableAddresses} available of{' '}
                      {subnet.availableAddresses + subnet.allocatedAddresses} assignable
                    </small>
                  </button>
                ))}
              </div>
            </div>
            <div className="capacity-footer">
              <span>
                <i className="legend-dot allocated" />
                {totals.allocated} allocated
              </span>
              <span>
                <i className="legend-dot available" />
                {totals.available} available
              </span>
              <span>
                <i className="legend-dot reserved" />
                {totals.reserved} reserved
              </span>
            </div>
          </section>
          <section className="panel inventory-panel">
            <div className="panel-heading">
              <div>
                <h2>Device inventory</h2>
                <p>Equipment by type</p>
              </div>
              <Icon name="devices" />
            </div>
            <div className="inventory-types">
              {deviceTypes.map((type) => {
                const count = devices.filter((device) => device.type === type).length
                return (
                  <div className="inventory-type" key={type}>
                    <span>{type.toLowerCase()}</span>
                    <div>
                      <span
                        style={{ width: `${devices.length ? (count / devices.length) * 100 : 0}%` }}
                      />
                    </div>
                    <strong>{count}</strong>
                  </div>
                )
              })}
            </div>
            <button className="inventory-footer" onClick={() => onNavigate('devices')}>
              <span>
                {unassigned} {unassigned === 1 ? 'interface' : 'interfaces'} without an address
              </span>
              <Icon name="arrow" size={16} />
            </button>
          </section>
        </div>
      )}

      <section className="section-heading">
        <div>
          <div className="eyebrow">ADDRESS INVENTORY</div>
          <h2>
            Managed subnets <span className="count">{subnets.length}</span>
          </h2>
        </div>
        <button className="text-button" onClick={() => onNavigate('subnets')}>
          View all subnets <Icon name="arrow" size={15} />
        </button>
      </section>
      <SubnetTable subnets={subnets.slice(0, 6)} onSelect={openSubnet} />
      <div className="workspace-note">
        <span className="live-dot" />
        <span>
          Inventory, not network discovery. Assignments are recorded here; devices are not
          configured automatically.
        </span>
      </div>
    </>
  )
}
