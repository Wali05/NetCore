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
import { Icon } from './components/Icon'
import { exportSubnets } from './export'

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
  const [updatedAt, setUpdatedAt] = useState<Date | null>(null)
  const [refreshing, setRefreshing] = useState(false)
  const [addressError, setAddressError] = useState('')
  const dismissError = useCallback(() => setError(''), [])
  const dismissMessage = useCallback(() => setMessage(''), [])

  const refresh = useCallback(async (signal?: AbortSignal) => {
    const [nextSubnets, nextDevices, nextInterfaces] = await Promise.all([
      api<Subnet[]>('/subnets', { signal }),
      api<Device[]>('/devices', { signal }),
      api<NetworkInterface[]>('/interfaces', { signal }),
    ])
    if (signal?.aborted) return
    setSubnets(nextSubnets)
    setDevices(nextDevices)
    setAllInterfaces(nextInterfaces)
    setConnected(true)
    setUpdatedAt(new Date())
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
    const controller = new AbortController()
    refresh(controller.signal)
      .catch((cause) => {
        if (controller.signal.aborted) return
        setConnected(false)
        setError(cause.message)
      })
      .finally(() => {
        if (!controller.signal.aborted) setLoading(false)
      })
    return () => controller.abort()
  }, [refresh])

  useEffect(() => {
    if (selectedSubnet === null || view !== 'subnets') {
      setAddresses(null)
      return
    }
    let cancelled = false
    const controller = new AbortController()
    setAddressError('')
    setAddresses(null)
    const query = new URLSearchParams({ page: String(page), size: String(PAGE_SIZE) })
    if (statusFilter !== 'ALL') query.set('status', statusFilter)
    api<Page<Address>>(`/subnets/${selectedSubnet}/addresses?${query}`, {
      signal: controller.signal,
    })
      .then((result) => {
        if (!cancelled) setAddresses(result)
      })
      .catch((cause) => {
        if (!cancelled) setAddressError(cause.message)
      })
    return () => {
      cancelled = true
      controller.abort()
    }
  }, [selectedSubnet, page, statusFilter, subnets, view])

  async function reload() {
    if (refreshing || busy) return
    setRefreshing(true)
    setError('')
    try {
      await refresh()
    } catch (cause) {
      setConnected(false)
      setError(cause instanceof Error ? cause.message : 'Could not refresh the inventory.')
    } finally {
      setRefreshing(false)
    }
  }

  function selectSubnet(id: number) {
    setSelectedSubnet(id)
    setPage(0)
    setStatusFilter('ALL')
  }

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
      const uncertainResult =
        cause instanceof TypeError ||
        (cause instanceof DOMException && cause.name === 'TimeoutError')
      if (uncertainResult) setConnected(false)
      setError(
        uncertainResult
          ? 'The request could not be confirmed. The change may have been saved; refresh the inventory before retrying.'
          : cause instanceof Error
            ? cause.message
            : 'Something went wrong',
      )
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
      <a className="skip-link" href="#main-content">
        Skip to content
      </a>
      <aside className="sidebar">
        <div className="brand">
          <span className="brand-symbol">
            <Icon name="network" size={25} />
          </span>
          <span className="brand-name">
            netcore<span>NETWORK WORKSPACE</span>
          </span>
        </div>
        <div className="workspace-selector">
          <span className="workspace-avatar">N</span>
          <div>
            Network inventory<small>IPv4 address management</small>
          </div>
        </div>
        <div className="nav-label">WORKSPACE</div>
        <nav aria-label="Main navigation">
          {(
            [
              ['overview', 'Overview', 'overview'],
              ['subnets', 'Address space', 'network'],
              ['devices', 'Devices', 'devices'],
            ] as const
          ).map(([id, label, icon]) => (
            <button
              key={id}
              className={`nav-item ${view === id ? 'active' : ''}`}
              onClick={() => setView(id)}
              aria-current={view === id ? 'page' : undefined}
            >
              <Icon name={icon} size={17} />
              {label}
              {id !== 'overview' && (
                <span className="nav-count">
                  {id === 'subnets' ? subnets.length : devices.length}
                </span>
              )}
            </button>
          ))}
        </nav>
        <div className="sidebar-foot">
          <span className="sidebar-foot-title">
            <span className="live-dot" /> One address. One assignment.
          </span>
          <small>Small networks, clearly managed.</small>
        </div>
      </aside>

      <main className="main" id="main-content" tabIndex={-1}>
        <header className="topbar">
          <span className="breadcrumb">
            NetCore <span>/</span>{' '}
            {view === 'subnets' ? 'Address space' : view === 'devices' ? 'Devices' : 'Overview'}
          </span>
          <div className="topbar-actions">
            <span className="last-updated">
              {updatedAt
                ? `Updated ${updatedAt.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}`
                : 'Waiting for inventory'}
            </span>
            <span
              className={`connection ${connected === false ? 'offline' : connected === null ? 'connecting' : ''}`}
            >
              <span className="live-dot" />{' '}
              {connected === null ? 'Connecting…' : connected ? 'API connected' : 'API unavailable'}
              {connected === false && (
                <button
                  className="retry"
                  disabled={refreshing || busy}
                  onClick={() => void reload()}
                >
                  Retry
                </button>
              )}
            </span>
            <button
              className="icon-button"
              aria-label="Refresh inventory"
              title="Refresh inventory"
              disabled={refreshing || busy || loading}
              onClick={() => void reload()}
            >
              <Icon name="refresh" size={16} />
            </button>
            <button
              className="secondary export-button"
              disabled={!subnets.length || loading}
              onClick={() => exportSubnets(subnets)}
            >
              <Icon name="download" size={15} /> Export CSV
            </button>
          </div>
        </header>

        {error && <Toast message={error} type="error" onDismiss={dismissError} />}
        {message && <Toast message={message} type="success" onDismiss={dismissMessage} />}
        {connected === false && updatedAt && (
          <div className="stale-notice" role="status">
            Connection lost. Showing the last loaded inventory. Refresh before making changes.
          </div>
        )}

        {loading ? (
          <div className="empty">Loading NetCore…</div>
        ) : connected === false && !updatedAt ? (
          <section className="connection-empty">
            <Icon name="network" size={38} />
            <h1>Let’s get connected.</h1>
            <p>
              The inventory could not be loaded. Check that the NetCore API is running, then try
              again.
            </p>
            <button className="primary" disabled={refreshing} onClick={() => void reload()}>
              {refreshing ? 'Connecting…' : 'Retry connection'}
            </button>
          </section>
        ) : (
          <>
            {view === 'overview' && (
              <OverviewView
                subnets={subnets}
                devices={devices}
                interfaces={allInterfaces}
                totals={totals}
                onNavigate={setView}
                onSelectSubnet={selectSubnet}
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
                addressError={addressError}
                onRetry={() => void reload()}
                allInterfaces={allInterfaces}
                page={page}
                statusFilter={statusFilter}
                busy={busy}
                unavailable={refreshing || connected !== true}
                showSubnetForm={showSubnetForm}
                onSelectSubnet={selectSubnet}
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
                unavailable={refreshing || connected !== true}
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
