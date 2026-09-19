export function isValidIpv4(value: string): boolean {
  const parts = value.split('.')
  if (parts.length !== 4) return false
  return parts.every((p) => {
    const n = Number(p)
    return /^\d{1,3}$/.test(p) && n >= 0 && n <= 255
  })
}

export function isValidMac(value: string): boolean {
  return /^([0-9A-Fa-f]{2}:){5}[0-9A-Fa-f]{2}$/.test(value)
}

export function ipv4Hint(value: string): string | null {
  if (!value) return null
  if (isValidIpv4(value)) return null
  const parts = value.split('.')
  if (parts.length < 4 && /^\d{1,3}\.?$/.test(parts[parts.length - 1])) {
    return 'Complete the address: A.B.C.D'
  }
  return 'Invalid IPv4 — use format A.B.C.D (0–255 each)'
}

export function macHint(value: string): string | null {
  if (!value) return null
  if (isValidMac(value)) return null
  if (value.length < 17) return 'Complete the address: XX:XX:XX:XX:XX:XX'
  return 'Invalid MAC — use format XX:XX:XX:XX:XX:XX'
}
