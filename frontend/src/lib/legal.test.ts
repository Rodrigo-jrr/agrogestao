import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'
import { describe, expect, it } from 'vitest'
import { TERMS_VERSION } from './legal'

describe('versão dos termos', () => {
  it('é a mesma no frontend e no backend (senão o cadastro é recusado)', () => {
    const java = readFileSync(
      resolve(__dirname, '../../../backend/src/main/java/com/agrogestao/auth/terms/TermsService.java'),
      'utf8',
    )
    const backendVersion = /CURRENT_VERSION = "([^"]+)"/.exec(java)?.[1]
    expect(backendVersion).toBe(TERMS_VERSION)
  })
})
