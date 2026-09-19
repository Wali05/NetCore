import { useEffect, useRef, useState, type FormEvent } from 'react'
import type { Address, IpStatus, NetworkInterface, Page, Subnet } from '../api'
import { ipv4Hint, isValidIpv4 } from '../validation'
import { CopyButton } from './CopyButton'
import { SubnetTable } from './SubnetTable'

interface AddressSpaceViewProps {
  subnets: Subnet[]
  selectedSubnet: number | null
  addresses: Page<Address> | null
  allInterfaces: NetworkInterface[]
  page: number
  statusFilter: 'ALL' | IpStatus
  busy: boolean
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
  allInterfaces,
  page,
  statusFilter,
  busy,
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
      <div className="two-column">
        <div className="stack">
          <section className="panel">
            <div className="panel-heading">
              <h2>Managed subnets</h2>
              <span className="count">{subnets.length}</span>
            </div>
            <SubnetTable
              subnets={subnets}
              selected={selectedSubnet}
              onSelect={(id) => {
                onSelectSubnet(id)
                onSetPage(0)
                setPendingRelease(null)
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
                  />
                  {networkHint && <span className="field-hint error">{networkHint}</span>}
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
                  />
                  {gatewayHint && <span className="field-hint error">{gatewayHint}</span>}
                </label>
                <p className="form-hint wide">
                  Network and broadcast addresses are reserved automatically. A gateway is reserved
                  when provided.
                </p>
                <button className="primary wide" disabled={busy || !!networkHint || !!gatewayHint}>
                  {busy ? 'Creating…' : 'Create subnet'}
                </button>
              </form>
            </section>
          )}
        </div>
        <div className="stack">
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
                />
                {requestedHint && <span className="field-hint error">{requestedHint}</span>}
              </label>
              <button
                className="primary wide"
                disabled={
                  busy ||
                  selectedSubnet === null ||
                  !allocationInterface ||
                  currentSubnet?.availableAddresses === 0 ||
                  !!requestedHint
                }
              >
                {busy ? 'Assigning…' : 'Assign address'}
              </button>
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
                      </button>
                    ))}
                  </div>
                  <span className="result-count">
                    {addresses ? `${addresses.totalElements} results` : 'Loading…'}
                  </span>
                </div>
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
                            : '—'}
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
                                disabled={busy}
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
                              disabled={busy}
                              onClick={() => setPendingRelease(address.address)}
                            >
                              Release
                            </button>
                          )}
                        </div>
                      )}
                    </div>
                  ))}
                  {addresses === null && <div className="empty small">Loading addresses…</div>}
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
