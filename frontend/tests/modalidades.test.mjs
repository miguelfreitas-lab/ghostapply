import assert from 'node:assert/strict'
import test from 'node:test'
import { createElement } from 'react'
import { renderToStaticMarkup } from 'react-dom/server'
import { createServer } from 'vite'

test('o formulário mostra HÍBRIDO com value HIBRIDO para a API', async () => {
  const vite = await createServer({
    server: { middlewareMode: true, watch: null, hmr: false },
    appType: 'custom',
  })

  try {
    const { default: Candidaturas } = await vite.ssrLoadModule('/src/Candidaturas.tsx')
    // Renderiza o formulário real; useEffect não executa nem consulta o backend no SSR.
    const html = renderToStaticMarkup(createElement(Candidaturas))
    const select = html.match(/<label>Modalidade<select\b[^>]*>([\s\S]*?)<\/select>/)
    assert.ok(select, 'O formulário deve renderizar o seletor de modalidade')

    const opcoes = [...select[1].matchAll(/<option\b[^>]*value="([^"]*)"[^>]*>([^<]*)<\/option>/g)]
      .map(([, value, label]) => ({ value, label }))

    // Sem value explícito o navegador usaria o rótulo acentuado como valor.
    assert.deepEqual(opcoes, [
      { value: 'REMOTO', label: 'REMOTO' },
      { value: 'HIBRIDO', label: 'HÍBRIDO' },
      { value: 'PRESENCIAL', label: 'PRESENCIAL' },
    ])
    assert.doesNotMatch(select[1], /value="HÍBRIDO"/)
  } finally {
    await vite.close()
  }
})
