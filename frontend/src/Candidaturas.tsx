import { useEffect, useState } from 'react'
import type { FormEvent } from 'react'

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
  const [itens, setItens] = useState<Candidatura[]>([])
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

  return <section aria-labelledby="candidaturas-titulo">
    <h2 id="candidaturas-titulo">Candidaturas</h2>
    {erro && <p role="alert" className="erro">{erro}</p>}
    <p role="status" className="sucesso">{sucesso}</p>
    <form onSubmit={salvar}>
      <h3>{editando === null ? 'Nova candidatura' : 'Editar candidatura'}</h3>
      <fieldset disabled={ocupado || carregando}>
        <label>Empresa<input required maxLength={200} value={dados.empresa} onChange={(e) => setDados({ ...dados, empresa: e.target.value })} /></label>
        <label>Título da vaga<input required maxLength={200} value={dados.titulo} onChange={(e) => setDados({ ...dados, titulo: e.target.value })} /></label>
        <label>URL da vaga (opcional)<input type="url" pattern="https?://.*" maxLength={2048} value={dados.urlVaga} onChange={(e) => setDados({ ...dados, urlVaga: e.target.value })} /></label>
        <label>Modalidade<select value={dados.modalidade} onChange={(e) => setDados({ ...dados, modalidade: e.target.value as Dados['modalidade'] })}>{modalidades.map((opcao) => <option key={opcao.value} value={opcao.value}>{opcao.label}</option>)}</select></label>
        <label>Status<select value={dados.status} onChange={(e) => setDados({ ...dados, status: e.target.value as Dados['status'] })}>{statusOpcoes.map((valor) => <option key={valor}>{valor}</option>)}</select></label>
        <div className="acoes"><button type="submit">{ocupado ? 'Aguarde...' : editando === null ? 'Cadastrar' : 'Salvar alterações'}</button>
          {editando !== null && <button type="button" onClick={() => { setEditando(null); setDados(vazio) }}>Cancelar edição</button>}</div>
      </fieldset>
    </form>
    <div className="lista-cabecalho"><h3>Candidaturas cadastradas ({itens.length})</h3><button disabled={carregando || ocupado} onClick={recarregar}>Atualizar lista</button></div>
    {carregando ? <p role="status">Carregando candidaturas...</p> : itens.length === 0 ? <p>Nenhuma candidatura para exibir.</p> :
      <ul className="candidaturas">{itens.map((item) => <li key={item.id}>
        <h3>{item.titulo}</h3><p>{item.empresa}</p>
        <p>{modalidades.find((opcao) => opcao.value === item.modalidade)?.label ?? item.modalidade} · {item.status}</p>
        <p>Criada em {new Date(item.criadaEm).toLocaleString('pt-BR')}</p>
        {item.urlVaga && /^https?:\/\//i.test(item.urlVaga) && <a href={item.urlVaga} target="_blank" rel="noopener noreferrer">Ver vaga</a>}
        <div className="acoes"><button disabled={ocupado || carregando} onClick={() => { setEditando(item.id); setDados({ empresa: item.empresa, titulo: item.titulo, urlVaga: item.urlVaga ?? '', modalidade: item.modalidade, status: item.status }); setErro(''); setSucesso('') }}>Editar</button>
          <button disabled={ocupado || carregando} onClick={() => excluir(item)}>Excluir</button></div>
      </li>)}</ul>}
  </section>
}
