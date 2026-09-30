import { describe, expect, it } from 'vitest'
import { safeNext } from './safeNext'

describe('safeNext', () => {
  it('aceita caminhos internos', () => {
    expect(safeNext('/convite')).toBe('/convite')
    expect(safeNext('/estoque?x=1')).toBe('/estoque?x=1')
  })

  it('recusa endereços que levariam para outro site', () => {
    expect(safeNext('https://malicioso.example')).toBeNull()
    expect(safeNext('//malicioso.example')).toBeNull()
    expect(safeNext('/\\malicioso.example')).toBeNull()
    expect(safeNext(null)).toBeNull()
  })
})
