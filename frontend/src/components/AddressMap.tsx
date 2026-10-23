import type { Address } from '../api'

interface AddressMapProps {
  addresses: Address[]
  selected: string
  disabled: boolean
  onChoose: (address: string) => void
}

export function AddressMap({ addresses, selected, disabled, onChoose }: AddressMapProps) {
  if (!addresses.length) return null
  return (
    <div className="address-map">
      <div className="address-map-heading">
        <strong>Page at a glance</strong>
        <span>
          {addresses.some((address) => address.status === 'AVAILABLE')
            ? 'Select an available IP to prepare its assignment.'
            : 'No available addresses on this page.'}
        </span>
      </div>
      <div className="address-tiles" aria-label="Addresses on this page">
        {addresses.map((address) => {
          const label = `${address.address} · ${address.status.toLowerCase()}${address.deviceName ? ` · ${address.deviceName}` : ''}`
          return address.status === 'AVAILABLE' ? (
            <button
              key={address.id}
              type="button"
              className={`address-tile available ${selected === address.address ? 'chosen' : ''}`}
              aria-label={`Choose ${address.address} for assignment`}
              aria-pressed={selected === address.address}
              title={label}
              disabled={disabled}
              onClick={() => onChoose(address.address)}
            >
              <span>{address.address.split('.').pop()}</span>
              <small>Free</small>
            </button>
          ) : (
            <div
              key={address.id}
              className={`address-tile ${address.status.toLowerCase()}`}
              title={label}
              aria-label={label}
            >
              <span>{address.address.split('.').pop()}</span>
              <small>{address.status === 'ALLOCATED' ? 'Used' : 'Rsvd'}</small>
            </div>
          )
        })}
      </div>
    </div>
  )
}
