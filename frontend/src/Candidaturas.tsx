import { useEffect, useRef, useState } from 'react'
import type { FormEvent } from 'react'
import { filtrarCandidaturas } from './buscaCandidaturas'

const modalidades = [
  { value: 'REMOTO', label: 'REMOTO' },
  { value: 'HIBRIDO', label: 'HÍBRIDO' },
  { value: 'PRESENCIAL', label: 'PRESENCIAL' },
] as const
const statusOpcoes = ['SALVA', 'INSCRITO', 'ENTREVISTA', 'OFERTA', 'REJEITADO'] as const
type Dados = {
  empresa: string
  titulo: string
  urlVaga: string
  modalidade: typeof modalidades[number]['value']
  status: typeof statusOpcoes[number]
}
type Candidatura = Omit<Dados, 'urlVaga'> & { id: number; criadaEm: string; urlVaga: string | null }
const vazio: Dados = { empresa: '', titulo: '', urlVaga: '', modalidade: 'REMOTO', status: 'SALVA' }

async function api<T>(path = '', options?: RequestInit): Promise<T> {
  const response = await fetch(`/api/candidaturas${path}`, options)
  if (!response.ok) {
    const body = await response.json().catch(() => null)
    throw new Error(body?.message || `Não foi possível concluir a operação (HTTP ${response.status}).`)
  }
  return response.status === 204 ? undefined as T : response.json()
}

export default function Candidaturas() {
  const empresaInput = useRef<HTMLInputElement>(null)
  const [itens, setItens] = useState<Candidatura[]>([])
  const [busca, setBusca] = useState('')
  const [statusFiltro, setStatusFiltro] = useState<Dados['status'] | ''>('')
  const [modalidadeFiltro, setModalidadeFiltro] = useState<Dados['modalidade'] | ''>('')
  const [dados, setDados] = useState<Dados>(vazio)
  const [editando, setEditando] = useState<number | null>(null)
  const [carregando, setCarregando] = useState(true)
  const [ocupado, setOcupado] = useState(false)
  const [erro, setErro] = useState('')
  const [sucesso, setSucesso] = useState('')

  useEffect(() => {
    const controller = new AbortController()
    api<Candidatura[]>('', { signal: controller.signal })
      .then((lista) => { if (!controller.signal.aborted) setItens(lista) })
      .catch(() => {
        if (!controller.signal.aborted) setErro('Não foi possível carregar as candidaturas. Verifique a conexão e tente novamente.')
      })
      .finally(() => { if (!controller.signal.aborted) setCarregando(false) })
    return () => controller.abort()
  }, [])

  async function recarregar() {
    setCarregando(true)
    setErro('')
    try { setItens(await api<Candidatura[]>()) }
    catch { setErro('Não foi possível carregar as candidaturas. Tente novamente.') }
    finally { setCarregando(false) }
  }

  async function salvar(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setOcupado(true)
    setErro('')
    setSucesso('')
    try {
      const candidatura = await api<Candidatura>(editando === null ? '' : `/${editando}`, {
        method: editando === null ? 'POST' : 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ ...dados, empresa: dados.empresa.trim(), titulo: dados.titulo.trim(), urlVaga: dados.urlVaga.trim() || null }),
      })
      setItens((atuais) => editando === null ? [candidatura, ...atuais] : atuais.map((item) => item.id === editando ? candidatura : item))
      setSucesso(editando === null ? 'Candidatura cadastrada.' : 'Candidatura atualizada.')
      setEditando(null)
      setDados(vazio)
    } catch (error) {
      setErro(error instanceof Error ? error.message : 'Erro de conexão.')
    } finally { setOcupado(false) }
  }

  async function excluir(item: Candidatura) {
    if (!window.confirm(`Excluir a candidatura para ${item.titulo} em ${item.empresa}?`)) return
    setOcupado(true)
    setErro('')
    setSucesso('')
    try {
      await api<void>(`/${item.id}`, { method: 'DELETE' })
      setItens((atuais) => atuais.filter((atual) => atual.id !== item.id))
      if (editando === item.id) { setEditando(null); setDados(vazio) }
      setSucesso('Candidatura excluída.')
    } catch (error) { setErro(error instanceof Error ? error.message : 'Erro de conexão.') }
    finally { setOcupado(false) }
  }

  const itensFiltrados = filtrarCandidaturas(itens, busca, {
    status: statusFiltro,
    modalidade: modalidadeFiltro,
  })

  const indicadores = [
    { label: 'Oportunidades no radar', valor: itens.length, classe: 'cream', detalhe: 'Todas as candidaturas' },
    { label: 'Em entrevista', valor: itens.filter((item) => item.status === 'ENTREVISTA').length, classe: 'lavender', detalhe: 'Conversas que abrem portas' },
    { label: 'Ofertas recebidas', valor: itens.filter((item) => item.status === 'OFERTA').length, classe: 'pink', detalhe: 'Motivos para comemorar' },
  ]

  return <section aria-labelledby="candidaturas-titulo">
    <div className="stats" aria-label="Resumo das candidaturas" aria-busy={carregando}>{indicadores.map((indicador) => <article className={`stat ${indicador.classe}`} key={indicador.label}><p>{indicador.label}</p><strong>{carregando || (erro && itens.length === 0) ? '—' : indicador.valor.toString().padStart(2, '0')}</strong><span>{indicador.detalhe}</span></article>)}</div>
    <div className="section-heading"><div><p className="eyebrow">SEU PRÓXIMO CAPÍTULO</p><h2 id="candidaturas-titulo">Oportunidades em movimento<span>.</span></h2></div><span className="section-index" aria-hidden="true">01 / SEU RADAR</span></div>
    {erro && <p role="alert" className="erro">{erro}</p>}
    <p role="status" className="sucesso">{sucesso}</p>
    <div className="workspace"><form id="formulario" onSubmit={salvar}>
      <p className="eyebrow">UM NOVO COMEÇO <span aria-hidden="true">↗</span></p>
      <h3 tabIndex={-1}>{editando === null ? 'Nova candidatura' : 'Editar candidatura'}</h3>
      <fieldset disabled={ocupado || carregando}>
        <label>Empresa<input ref={empresaInput} placeholder="Ex.: Empresa dos sonhos" required maxLength={200} value={dados.empresa} onChange={(e) => setDados({ ...dados, empresa: e.target.value })} /></label>
        <label>Título da vaga<input placeholder="Ex.: Desenvolvedor(a) frontend" required maxLength={200} value={dados.titulo} onChange={(e) => setDados({ ...dados, titulo: e.target.value })} /></label>
        <label className="full-width">URL da vaga (opcional)<input placeholder="https://" type="url" pattern="https?://.*" maxLength={2048} value={dados.urlVaga} onChange={(e) => setDados({ ...dados, urlVaga: e.target.value })} /></label>
        <label>Modalidade<select value={dados.modalidade} onChange={(e) => setDados({ ...dados, modalidade: e.target.value as Dados['modalidade'] })}>{modalidades.map((opcao) => <option key={opcao.value} value={opcao.value}>{opcao.label}</option>)}</select></label>
        <label>Status<select value={dados.status} onChange={(e) => setDados({ ...dados, status: e.target.value as Dados['status'] })}>{statusOpcoes.map((valor) => <option key={valor}>{valor}</option>)}</select></label>
        <div className="acoes full-width"><button className="primary" type="submit">{ocupado ? 'Aguarde...' : editando === null ? 'Cadastrar' : 'Salvar alterações'}</button>
          {editando !== null && <button type="button" onClick={() => { setEditando(null); setDados(vazio) }}>Cancelar edição</button>}</div>
      </fieldset>
    </form>
    <div className="list-panel" aria-busy={carregando}><div className="lista-cabecalho"><h3>Seu radar <span className="count" role="status">{itensFiltrados.length} de {itens.length}</span></h3><button disabled={carregando || ocupado} onClick={recarregar}>Atualizar lista</button></div>
    <div role="group" aria-label="Filtros de candidaturas">
    <label className="busca-candidaturas" htmlFor="busca-candidaturas">Buscar por empresa ou título
      <input id="busca-candidaturas" type="search" placeholder="Ex.: empresa ou desenvolvedor" value={busca} onChange={(event) => setBusca(event.target.value)} />
    </label>
    <div className="seletores-filtros">
      <label htmlFor="filtro-status">Filtrar por status
        <select id="filtro-status" value={statusFiltro} onChange={(event) => setStatusFiltro(event.target.value as Dados['status'] | '')}>
          <option value="">Todos</option>
          {statusOpcoes.map((valor) => <option key={valor} value={valor}>{valor.charAt(0) + valor.slice(1).toLowerCase()}</option>)}
        </select>
      </label>
      <label htmlFor="filtro-modalidade">Filtrar por modalidade
        <select id="filtro-modalidade" value={modalidadeFiltro} onChange={(event) => setModalidadeFiltro(event.target.value as Dados['modalidade'] | '')}>
          <option value="">Todas</option>
          {modalidades.map((opcao) => <option key={opcao.value} value={opcao.value}>{opcao.label.charAt(0) + opcao.label.slice(1).toLowerCase()}</option>)}
        </select>
      </label>
    </div>
    </div>
    {carregando ? <p role="status">Carregando candidaturas...</p> : itens.length === 0 ? <div className="empty-state"><span aria-hidden="true">✳</span><h3>{erro ? 'Seu radar está indisponível' : 'Tudo começa com uma oportunidade.'}</h3><p>{erro ? 'Tente atualizar a lista para carregar suas candidaturas.' : 'Encontrou uma vaga interessante? Registre ao lado e acompanhe por aqui.'}</p></div> :
      itensFiltrados.length === 0 ? <div className="empty-state"><span aria-hidden="true">✳</span><h3>Nenhuma candidatura encontrada.</h3><p>Tente outro texto ou ajuste os filtros de status e modalidade.</p></div> :
      <ul className="candidaturas">{itensFiltrados.map((item) => <li key={item.id}>
        <div className="card-top"><span className="company-avatar" aria-hidden="true">{item.empresa.slice(0, 1).toUpperCase()}</span><span className={`status status-${item.status.toLowerCase()}`}>{item.status}</span></div><p className="company-name">{item.empresa}</p><h3>{item.titulo}</h3>
        <p>{modalidades.find((opcao) => opcao.value === item.modalidade)?.label ?? item.modalidade} · {item.status}</p>
        <p>Criada em {new Date(item.criadaEm).toLocaleString('pt-BR')}</p>
        {item.urlVaga && /^https?:\/\//i.test(item.urlVaga) && <a href={item.urlVaga} target="_blank" rel="noopener noreferrer">Ver vaga</a>}
        <div className="acoes"><button disabled={ocupado || carregando} onClick={() => { setEditando(item.id); setDados({ empresa: item.empresa, titulo: item.titulo, urlVaga: item.urlVaga ?? '', modalidade: item.modalidade, status: item.status }); setErro(''); setSucesso(''); empresaInput.current?.focus() }}>Editar</button>
          <button disabled={ocupado || carregando} className="danger" onClick={() => excluir(item)}>Excluir</button></div>
      </li>)}</ul>}</div></div>
  </section>
}
