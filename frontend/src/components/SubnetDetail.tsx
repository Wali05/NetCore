import type { Subnet } from '../api'
import { Icon } from './Icon'

export function SubnetDetail({ subnet }: { subnet: Subnet }) {
  const octets = subnet.networkAddress.split('.').map(Number)
  // Supported pools are /24–/30, so their bounds stay within the last octet.
  const broadcast = [...octets.slice(0, 3), octets[3] + 2 ** (32 - subnet.prefixLength) - 1].join(
    '.',
  )
  return (
    <section className="subnet-detail" aria-label="Selected subnet details">
      <div className="subnet-detail-title">
        <span className="detail-symbol">
          <Icon name="network" size={24} />
        </span>
        <div>
          <div className="eyebrow">SELECTED ADDRESS SPACE</div>
          <h2 className="mono">{subnet.cidr}</h2>
        </div>
        <span className={`capacity-badge ${subnet.utilizationPercent >= 80 ? 'high' : ''}`}>
          {subnet.availableAddresses === 0
            ? 'Pool full'
            : subnet.utilizationPercent >= 80
              ? 'Near capacity'
              : 'Space available'}
        </span>
      </div>
      <dl className="subnet-facts">
        <div>
          <dt>Gateway</dt>
          <dd className="mono">{subnet.gatewayAddress ?? 'Not configured'}</dd>
        </div>
        <div>
          <dt>Broadcast</dt>
          <dd className="mono">{broadcast}</dd>
        </div>
        <div>
          <dt>Available</dt>
          <dd>
            {subnet.availableAddresses}
            <span> {subnet.availableAddresses === 1 ? 'address' : 'addresses'}</span>
          </dd>
        </div>
        <div>
          <dt>Reserved</dt>
          <dd>
            {subnet.reservedAddresses}
            <span> addresses</span>
          </dd>
        </div>
      </dl>
      <div
        className="pool-meter"
        role="img"
        aria-label={`${subnet.allocatedAddresses} allocated, ${subnet.availableAddresses} available, ${subnet.reservedAddresses} reserved`}
      >
        <span
          className="allocated"
          style={{ width: `${(subnet.allocatedAddresses / subnet.totalAddresses) * 100}%` }}
        />
        <span
          className="available"
          style={{ width: `${(subnet.availableAddresses / subnet.totalAddresses) * 100}%` }}
        />
        <span
          className="reserved"
          style={{ width: `${(subnet.reservedAddresses / subnet.totalAddresses) * 100}%` }}
        />
      </div>
    </section>
  )
}
