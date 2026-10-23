import { useEffect, useRef, useState, type FormEvent } from 'react'
import type { Address, IpStatus, NetworkInterface, Page, Subnet } from '../api'
import { ipv4Hint, isValidIpv4 } from '../validation'
import { CopyButton } from './CopyButton'
import { SubnetTable } from './SubnetTable'
import { SubnetDetail } from './SubnetDetail'
import { AddressMap } from './AddressMap'

interface AddressSpaceViewProps {
  subnets: Subnet[]
  selectedSubnet: number | null
  addresses: Page<Address> | null
  addressError: string
  onRetry: () => void
  allInterfaces: NetworkInterface[]
  page: number
  statusFilter: 'ALL' | IpStatus
  busy: boolean
  unavailable: boolean
  showSubnetForm: boolean
  onSelectSubnet: (id: number) => void
  onSetPage: (page: number) => void
  onSetStatusFilter: (status: 'ALL' | IpStatus) => void
  onSetShowSubnetForm: (open: boolean) => void
  onCreateSubnet: (network: string, prefix: number, gateway: string) => Promise<boolean>
  onAllocate: (interfaceId: string, requestedAddress: string) => Promise<boolean>
  onRelease: (address: string) => void
  onNavigate: (view: 'devices') => void
}

export function AddressSpaceView({
  subnets,
  selectedSubnet,
  addresses,
  addressError,
  onRetry,
  allInterfaces,
  page,
  statusFilter,
  busy,
  unavailable,
  showSubnetForm,
  onSelectSubnet,
  onSetPage,
  onSetStatusFilter,
  onSetShowSubnetForm,
  onCreateSubnet,
  onAllocate,
  onRelease,
  onNavigate,
}: AddressSpaceViewProps) {
  const currentSubnet = subnets.find((s) => s.id === selectedSubnet)
  const subnetInput = useRef<HTMLInputElement>(null)
  const interfaceInput = useRef<HTMLSelectElement>(null)

  const [network, setNetwork] = useState('')
  const [prefix, setPrefix] = useState(29)
  const [gateway, setGateway] = useState('')
  const [allocationInterface, setAllocationInterface] = useState('')
  const [requestedAddress, setRequestedAddress] = useState('')
  const [pendingRelease, setPendingRelease] = useState<string | null>(null)
  const [networkTouched, setNetworkTouched] = useState(false)
  const [gatewayTouched, setGatewayTouched] = useState(false)
  const [requestedTouched, setRequestedTouched] = useState(false)

  useEffect(() => {
    if (showSubnetForm) subnetInput.current?.focus()
  }, [showSubnetForm])

  const networkHint = networkTouched ? ipv4Hint(network) : null
  const gatewayHint = gatewayTouched ? ipv4Hint(gateway) : null
  const requestedHint = requestedTouched ? ipv4Hint(requestedAddress) : null
  const statusCounts = currentSubnet
    ? {
        ALL: currentSubnet.totalAddresses,
        AVAILABLE: currentSubnet.availableAddresses,
        ALLOCATED: currentSubnet.allocatedAddresses,
        RESERVED: currentSubnet.reservedAddresses,
      }
    : null

  async function handleCreateSubnet(event: FormEvent) {
    event.preventDefault()
    if (!isValidIpv4(network.trim())) return
    if (gateway.trim() && !isValidIpv4(gateway.trim())) return
    if (!(await onCreateSubnet(network.trim(), prefix, gateway.trim()))) return
    setNetwork('')
    setGateway('')
    setNetworkTouched(false)
    setGatewayTouched(false)
    onSetShowSubnetForm(false)
  }

  async function handleAllocate(event: FormEvent) {
    event.preventDefault()
    if (requestedAddress.trim() && !isValidIpv4(requestedAddress.trim())) return
    if (!(await onAllocate(allocationInterface, requestedAddress.trim()))) return
    setRequestedAddress('')
    setRequestedTouched(false)
  }

  return (
    <>
      <section className="page-heading">
        <div>
          <div className="eyebrow">ADDRESS SPACE</div>
          <h1>Subnets &amp; assignments</h1>
          <p>Choose a pool, assign an address, or inspect what is in use.</p>
        </div>
        <button className="primary" onClick={() => onSetShowSubnetForm(!showSubnetForm)}>
          {showSubnetForm ? 'Close form' : '+ New subnet'}
        </button>
      </section>
      {currentSubnet && <SubnetDetail subnet={currentSubnet} />}
      <div className="two-column">
        <div className="stack">
          <section className="panel">
            <div className="panel-heading">
              <h2>Managed subnets</h2>
              <span className="count">{subnets.length}</span>
            </div>
            <SubnetTable
              searchable
              subnets={subnets}
              selected={selectedSubnet}
              onSelect={(id) => {
                onSelectSubnet(id)
                onSetPage(0)
                setPendingRelease(null)
                setRequestedAddress('')
                setRequestedTouched(false)
              }}
            />
          </section>
          {(showSubnetForm || subnets.length === 0) && (
            <section className="panel" id="new-subnet">
              <div className="panel-heading">
                <h2>New subnet</h2>
                <span className="muted">IPv4 · /24 to /30</span>
              </div>
              <form onSubmit={handleCreateSubnet} className="form-grid">
                <label>
                  Network address
                  <input
                    ref={subnetInput}
                    required
                    placeholder="192.168.10.0"
                    value={network}
                    onChange={(e) => setNetwork(e.target.value)}
                    onBlur={() => setNetworkTouched(true)}
                    className={networkHint ? 'field-invalid' : ''}
                    aria-invalid={!!networkHint}
                    aria-describedby={networkHint ? 'network-error' : undefined}
                  />
                  {networkHint && (
                    <span id="network-error" className="field-hint error">
                      {networkHint}
                    </span>
                  )}
                </label>
                <label>
                  Prefix length
                  <select value={prefix} onChange={(e) => setPrefix(Number(e.target.value))}>
                    {[24, 25, 26, 27, 28, 29, 30].map((v) => (
                      <option key={v} value={v}>
                        /{v}
                      </option>
                    ))}
                  </select>
                </label>
                <label className="wide">
                  <span className="label-line">
                    Gateway address <span className="muted">optional</span>
                  </span>
                  <input
                    placeholder="192.168.10.1"
                    value={gateway}
                    onChange={(e) => setGateway(e.target.value)}
                    onBlur={() => setGatewayTouched(true)}
                    className={gatewayHint ? 'field-invalid' : ''}
                    aria-invalid={!!gatewayHint}
                    aria-describedby={gatewayHint ? 'gateway-error' : undefined}
                  />
                  {gatewayHint && (
                    <span id="gateway-error" className="field-hint error">
                      {gatewayHint}
                    </span>
                  )}
                </label>
                <p className="form-hint wide">
                  Network and broadcast addresses are reserved automatically. A gateway is reserved
                  when provided.
                </p>
                <button
                  className="primary wide"
                  disabled={busy || unavailable || !!networkHint || !!gatewayHint}
                >
                  {busy ? 'Creating…' : 'Create subnet'}
                </button>
              </form>
            </section>
          )}
          <section className="panel assignment-panel">
            <div className="panel-heading">
              <div>
                <h2>Assign an address</h2>
                <p>{currentSubnet ? `From ${currentSubnet.cidr}` : 'Select a subnet to begin'}</p>
              </div>
            </div>
            <form onSubmit={handleAllocate} className="form-grid">
              <label className="wide">
                Device interface
                <select
                  ref={interfaceInput}
                  required
                  value={allocationInterface}
                  onChange={(e) => setAllocationInterface(e.target.value)}
                >
                  <option value="">Choose an interface</option>
                  {allInterfaces.map((item) => (
                    <option key={item.id} value={item.id}>
                      {item.deviceName} / {item.name}
                    </option>
                  ))}
                </select>
              </label>
              <label className="wide">
                <span className="label-line">
                  Specific IP <span className="muted">optional</span>
                </span>
                <input
                  placeholder="Leave blank for next available"
                  value={requestedAddress}
                  onChange={(e) => setRequestedAddress(e.target.value)}
                  onBlur={() => setRequestedTouched(true)}
                  className={requestedHint ? 'field-invalid' : ''}
                  aria-invalid={!!requestedHint}
                  aria-describedby={requestedHint ? 'requested-error' : undefined}
                />
                {requestedHint && (
                  <span id="requested-error" className="field-hint error">
                    {requestedHint}
                  </span>
                )}
              </label>
              <button
                className="primary wide"
                disabled={
                  busy ||
                  unavailable ||
                  selectedSubnet === null ||
                  !allocationInterface ||
                  currentSubnet?.availableAddresses === 0 ||
                  !!requestedHint
                }
              >
                {busy ? 'Assigning…' : 'Assign address'}
              </button>
              <p className="form-hint wide">
                {requestedAddress
                  ? `Requesting ${requestedAddress} in ${currentSubnet?.cidr ?? 'the selected subnet'}.`
                  : 'Automatically selects the lowest available address in this subnet.'}{' '}
                Availability is checked again when you assign.
              </p>
              {allInterfaces.length === 0 && (
                <p className="form-hint wide">
                  Add a device and interface in{' '}
                  <button
                    type="button"
                    className="inline-link"
                    onClick={() => onNavigate('devices')}
                  >
                    Devices
                  </button>{' '}
                  first.
                </p>
              )}
            </form>
          </section>
        </div>
        <div className="stack">
          <section className="panel">
            <div className="panel-heading">
              <div>
                <h2>Address pool</h2>
                <p>
                  {currentSubnet
                    ? `${currentSubnet.cidr} · ${currentSubnet.availableAddresses} available · ${currentSubnet.allocatedAddresses} allocated`
                    : 'Select a subnet to inspect its addresses'}
                </p>
              </div>
            </div>
            {currentSubnet && (
              <>
                <div className="filters">
                  <div className="filter-options" aria-label="Filter addresses by status">
                    {(['ALL', 'AVAILABLE', 'ALLOCATED', 'RESERVED'] as const).map((status) => (
                      <button
                        key={status}
                        className={statusFilter === status ? 'active' : ''}
                        aria-pressed={statusFilter === status}
                        onClick={() => {
                          onSetStatusFilter(status)
                          onSetPage(0)
                          setPendingRelease(null)
                        }}
                      >
                        {status === 'ALL' ? 'All' : status.toLowerCase()}
                        <span className="filter-count">{statusCounts?.[status]}</span>
                      </button>
                    ))}
                  </div>
                  <span className="result-count">
                    {addresses ? `${addresses.totalElements} results` : 'Loading…'}
                  </span>
                </div>
                {addresses && (
                  <AddressMap
                    addresses={addresses.content}
                    selected={requestedAddress}
                    disabled={busy || unavailable}
                    onChoose={(address) => {
                      setRequestedAddress(address)
                      setRequestedTouched(false)
                      interfaceInput.current?.focus()
                    }}
                  />
                )}
                <div className="address-list">
                  {addresses?.content.map((address) => (
                    <div className="address-row" key={address.id}>
                      <div>
                        <span className="copyable-group">
                          <strong className="mono">{address.address}</strong>
                          <CopyButton text={address.address} />
                        </span>
                        <small>
                          {address.deviceName
                            ? `${address.deviceName} / ${address.networkInterfaceName}`
                            : address.status === 'AVAILABLE'
                              ? 'Ready for assignment'
                              : address.address === currentSubnet.gatewayAddress
                                ? 'Gateway reservation'
                                : address.address === currentSubnet.networkAddress
                                  ? 'Network address'
                                  : 'Broadcast address'}
                        </small>
                      </div>
                      <span className={`pill ${address.status.toLowerCase()}`}>
                        {address.status.toLowerCase()}
                      </span>
                      {address.status === 'ALLOCATED' && (
                        <div className="row-actions">
                          {pendingRelease === address.address ? (
                            <>
                              <button
                                className="row-action confirm"
                                disabled={busy || unavailable}
                                onClick={() => {
                                  onRelease(address.address)
                                  setPendingRelease(null)
                                }}
                              >
                                Confirm
                              </button>
                              <button
                                className="row-action"
                                onClick={() => setPendingRelease(null)}
                              >
                                Cancel
                              </button>
                            </>
                          ) : (
                            <button
                              className="row-action"
                              disabled={busy || unavailable}
                              onClick={() => setPendingRelease(address.address)}
                            >
                              Release
                            </button>
                          )}
                        </div>
                      )}
                    </div>
                  ))}
                  {addressError ? (
                    <div className="empty small" role="alert">
                      {addressError}{' '}
                      <button className="text-button" onClick={onRetry} disabled={busy}>
                        Try again
                      </button>
                    </div>
                  ) : (
                    addresses === null && <div className="empty small">Loading addresses…</div>
                  )}
                  {addresses?.content.length === 0 && (
                    <div className="empty small">No addresses match this filter.</div>
                  )}
                </div>
                <div className="pager">
                  <span>
                    Page {page + 1} of {Math.max(1, addresses?.totalPages ?? 1)}
                  </span>
                  <div>
                    <button
                      aria-label="Previous address page"
                      disabled={page === 0 || addresses === null}
                      onClick={() => onSetPage(page - 1)}
                    >
                      ←
                    </button>
                    <button
                      aria-label="Next address page"
                      disabled={addresses === null || page + 1 >= addresses.totalPages}
                      onClick={() => onSetPage(page + 1)}
                    >
                      →
                    </button>
                  </div>
                </div>
              </>
            )}
          </section>
        </div>
      </div>
    </>
  )
}
