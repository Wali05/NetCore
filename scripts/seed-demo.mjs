// Uses the public API; never runs automatically or overwrites an existing inventory.
const base = (process.argv[2] || 'http://127.0.0.1:8080').replace(/\/$/, '')
const target = new URL(base)
if (!['localhost', '127.0.0.1', '[::1]'].includes(target.hostname)) {
  throw new Error('Demo seeding is limited to localhost. Use an SSH tunnel for a remote demo.')
}

async function request(path, body) {
  const response = await fetch(`${base}/api/v1${path}`, {
    method: body ? 'POST' : 'GET',
    headers: { 'Content-Type': 'application/json' },
    body: body ? JSON.stringify(body) : undefined,
    signal: AbortSignal.timeout(15000),
  })
  if (!response.ok) throw new Error(`${response.status}: ${await response.text()}`)
  return response.json()
}

const [existingSubnets, existingDevices] = await Promise.all([request('/subnets'), request('/devices')])
if (existingSubnets.length || existingDevices.length) {
  console.log('Inventory is not empty. Nothing changed; use an empty development database for the sample.')
  process.exit(0)
}

const networks = [
  ['10.20.10.0', 27, '10.20.10.1', 11],
  ['10.20.20.0', 28, '10.20.20.1', 5],
  ['10.20.30.0', 29, '10.20.30.1', 4],
  ['10.20.40.0', 26, '10.20.40.1', 3],
]
const equipment = [
  ['edge-router-01', 'ROUTER'], ['core-switch-01', 'SWITCH'],
  ['compute-node-01', 'SERVER'], ['compute-node-02', 'SERVER'],
  ['storage-node-01', 'SERVER'], ['build-runner-01', 'SERVER'],
  ['ops-workstation-01', 'WORKSTATION'], ['monitoring-01', 'OTHER'],
]
const interfaces = []
for (const [index, [name, type]] of equipment.entries()) {
  const device = await request('/devices', { name, type })
  interfaces.push(await request(`/devices/${device.id}/interfaces`, {
    name: 'eth0', macAddress: `02:4E:43:00:00:${index.toString(16).padStart(2, '0')}`,
  }))
}
for (const [networkAddress, prefixLength, gatewayAddress, allocations] of networks) {
  const subnet = await request('/subnets', { networkAddress, prefixLength, gatewayAddress })
  for (let i = 0; i < allocations; i++) {
    // An interface can own several addresses; leave the last interface unassigned.
    await request(`/subnets/${subnet.id}/allocate-next`, { networkInterfaceId: interfaces[i % 7].id })
  }
}
console.log('Sample inventory created: 4 subnets, 8 devices, 8 interfaces, 23 allocations. Refresh the dashboard.')
