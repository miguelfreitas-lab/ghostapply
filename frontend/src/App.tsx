
import { useEffect, useState } from 'react'
import Candidaturas from './Candidaturas'
import Ghost from './Ghost'
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
    <>
      <a className="skip-link" href="#conteudo">Pular para o conteúdo</a>
      <header className="site-header">
        <a className="brand" href="#" aria-label="GhostApply — início"><Ghost />GhostApply<span className="version">v0.2</span></a>
        <nav aria-label="Navegação principal"><a href="#candidaturas-titulo">Minhas candidaturas <span aria-hidden="true">↗</span></a></nav>
      </header>
      <main id="conteudo">
        <section className="hero" aria-labelledby="hero-titulo">
          <div><p className="eyebrow"><span className="little-star" aria-hidden="true">✳</span> MENOS CAOS. MAIS POSSIBILIDADES.</p>
            <h1 id="hero-titulo">Seu próximo passo.<br /><span>Sem assombração.</span></h1>
            <p className="hero-description">Uma casa para todas as suas candidaturas.<br />Organize as oportunidades e acompanhe cada conquista.</p>
            <a className="button primary" href="#formulario">Registrar candidatura <span aria-hidden="true">＋</span></a>
          </div>
          <aside className="hero-note"><span className="note-label">UM LEMBRETE AMIGO</span><Ghost className="hero-ghost" /><p>O futuro dá um frio na barriga.<br /><strong>A organização dá uma força.</strong></p><span className="note-spark" aria-hidden="true">✦</span></aside>
        </section>
        <Candidaturas />
      </main>
      <footer className="site-footer"><span className="footer-brand">GhostApply</span><p>Um passo de cada vez. Uma oportunidade de cada vez.</p>
        <p className={`connection ${connection.state}`} role="status">
          <span aria-hidden="true">● </span>
          {connection.state === 'loading' && 'Verificando conexão…'}
          {connection.state === 'connected' && 'API conectada'}
          {connection.state === 'error' && 'API indisponível · tente recarregar a página'}
        </p>
      </footer>
    </>
  )
}

export default App
