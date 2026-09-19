import { useState, type FormEvent } from 'react'
import type { Device, DeviceType, NetworkInterface } from '../api'
import { isValidMac, macHint } from '../validation'
import { CopyButton } from './CopyButton'

const deviceTypes: DeviceType[] = ['SERVER', 'WORKSTATION', 'ROUTER', 'SWITCH', 'OTHER']

interface DevicesViewProps {
  devices: Device[]
  selectedDevice: number | null
  interfaces: NetworkInterface[]
  busy: boolean
  onSelectDevice: (id: number) => void
  onCreateDevice: (name: string, type: DeviceType) => Promise<boolean>
  onCreateInterface: (deviceId: number, name: string, mac: string) => Promise<boolean>
}

export function DevicesView({
  devices,
  selectedDevice,
  interfaces,
  busy,
  onSelectDevice,
  onCreateDevice,
  onCreateInterface,
}: DevicesViewProps) {
  const [deviceName, setDeviceName] = useState('')
  const [deviceType, setDeviceType] = useState<DeviceType>('SERVER')
  const [interfaceName, setInterfaceName] = useState('')
  const [mac, setMac] = useState('')
  const [macTouched, setMacTouched] = useState(false)

  const macValidation = macTouched ? macHint(mac) : null

  async function handleCreateDevice(event: FormEvent) {
    event.preventDefault()
    if (!(await onCreateDevice(deviceName.trim(), deviceType))) return
    setDeviceName('')
  }

  async function handleCreateInterface(event: FormEvent) {
    event.preventDefault()
    if (selectedDevice === null) return
    if (mac.trim() && !isValidMac(mac.trim())) return
    if (!(await onCreateInterface(selectedDevice, interfaceName.trim(), mac.trim()))) return
    setInterfaceName('')
    setMac('')
    setMacTouched(false)
  }

  const selectedDeviceData = devices.find((d) => d.id === selectedDevice)

  return (
    <>
      <section className="page-heading">
        <div>
          <div className="eyebrow">HARDWARE INVENTORY</div>
          <h1>Devices</h1>
          <p>Register equipment and the interfaces that receive IP addresses.</p>
        </div>
      </section>
      <div className="two-column">
        <div className="stack">
          <section className="panel">
            <div className="panel-heading">
              <h2>Registered devices</h2>
              <span className="count">{devices.length}</span>
            </div>
            <div className="device-list">
              {devices.map((device) => (
                <button
                  className={`device-row ${selectedDevice === device.id ? 'selected' : ''}`}
                  key={device.id}
                  onClick={() => onSelectDevice(device.id)}
                  aria-pressed={selectedDevice === device.id}
                >
                  <span className="device-icon" aria-hidden="true">
                    {device.name.slice(0, 1).toUpperCase()}
                  </span>
                  <span>
                    <strong>{device.name}</strong>
                    <small>{device.type.toLowerCase()}</small>
                  </span>
                  <span className="chevron">→</span>
                </button>
              ))}
              {devices.length === 0 && (
                <div className="empty small">No devices yet. Add one below.</div>
              )}
            </div>
          </section>
          <section className="panel">
            <div className="panel-heading">
              <h2>New device</h2>
            </div>
            <form onSubmit={handleCreateDevice} className="form-grid">
              <label className="wide">
                Device name
                <input
                  required
                  placeholder="edge-router-01"
                  value={deviceName}
                  onChange={(e) => setDeviceName(e.target.value)}
                />
              </label>
              <label className="wide">
                Type
                <select
                  value={deviceType}
                  onChange={(e) => setDeviceType(e.target.value as DeviceType)}
                >
                  {deviceTypes.map((type) => (
                    <option key={type} value={type}>
                      {type.toLowerCase()}
                    </option>
                  ))}
                </select>
              </label>
              <button className="primary wide" disabled={busy}>
                {busy ? 'Creating…' : 'Create device'}
              </button>
            </form>
          </section>
        </div>
        <div className="stack">
          <section className="panel">
            <div className="panel-heading">
              <div>
                <h2>{selectedDeviceData?.name ?? 'Interfaces'}</h2>
                <p>Interfaces and their current address assignments</p>
              </div>
            </div>
            <div className="interface-list">
              {interfaces.map((item) => (
                <div className="interface-row" key={item.id}>
                  <div className="interface-top">
                    <strong>{item.name}</strong>
                    <span className="copyable-group">
                      <span className="mono muted">{item.macAddress}</span>
                      <CopyButton text={item.macAddress} />
                    </span>
                  </div>
                  <div className="assignment-list">
                    {item.assignedAddresses.length ? (
                      item.assignedAddresses.map((assignment) => (
                        <span className="copyable-group" key={assignment.address}>
                          <span className="assignment mono">{assignment.address}</span>
                          <CopyButton text={assignment.address} />
                        </span>
                      ))
                    ) : (
                      <span className="muted">No IP assigned</span>
                    )}
                  </div>
                </div>
              ))}
              {selectedDevice !== null && interfaces.length === 0 && (
                <div className="empty small">No interfaces for this device yet.</div>
              )}
              {selectedDevice === null && (
                <div className="empty small">Select or create a device.</div>
              )}
            </div>
          </section>
          <section className="panel">
            <div className="panel-heading">
              <h2>Add interface</h2>
            </div>
            <form onSubmit={handleCreateInterface} className="form-grid">
              <label className="wide">
                Interface name
                <input
                  required
                  placeholder="eth0"
                  value={interfaceName}
                  onChange={(e) => setInterfaceName(e.target.value)}
                />
              </label>
              <label className="wide">
                MAC address
                <input
                  required
                  placeholder="AA:BB:CC:DD:EE:01"
                  value={mac}
                  onChange={(e) => setMac(e.target.value)}
                  onBlur={() => setMacTouched(true)}
                  className={macValidation ? 'field-invalid' : ''}
                />
                {macValidation && <span className="field-hint error">{macValidation}</span>}
              </label>
              <button
                className="primary wide"
                disabled={busy || selectedDevice === null || !!macValidation}
              >
                {busy ? 'Adding…' : 'Add interface'}
              </button>
            </form>
          </section>
        </div>
      </div>
    </>
  )
}
