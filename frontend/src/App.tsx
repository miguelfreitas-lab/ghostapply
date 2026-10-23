
import { useEffect, useState } from 'react'
import Candidaturas from './Candidaturas'
import './App.css'

type ConnectionState =
  | { state: 'loading' }
  | { state: 'connected'; status: string; application: string }
  | { state: 'error' }

function App() {
  const [connection, setConnection] = useState<ConnectionState>({ state: 'loading' })

  useEffect(() => {
    const controller = new AbortController()

    fetch('/api/health', { signal: controller.signal })
      .then((response) => {
        if (!response.ok) {
          throw new Error('Erro ao consultar API')
        }
        return response.json()
      })
      .then((data: unknown) => {
        if (
          typeof data !== 'object' ||
          data === null ||
          !('status' in data) ||
          data.status !== 'UP' ||
          !('application' in data) ||
          typeof data.application !== 'string'
        ) {
          throw new Error('Resposta de saúde inválida')
        }

        if (!controller.signal.aborted) {
          setConnection({
            state: 'connected',
            status: data.status,
            application: data.application,
          })
        }
      })
      .catch(() => {
        if (!controller.signal.aborted) {
          setConnection({ state: 'error' })
        }
      })

    return () => controller.abort()
  }, [])

  return (
    <main>
      <h1>GhostApply</h1>
      <p>Plataforma inteligente de candidaturas</p>
      <h2>Status do backend</h2>
      <p role="status" aria-live="polite">
        {connection.state === 'loading' && 'Conectando ao backend...'}
        {connection.state === 'connected' &&
          `Conectado a ${connection.application} — Status: ${connection.status}`}
        {connection.state === 'error' &&
          'Erro de conexão. Verifique se o backend está disponível em http://localhost:8080 e recarregue a página.'}
      </p>
      <Candidaturas />
    </main>
  )
}

export default App
