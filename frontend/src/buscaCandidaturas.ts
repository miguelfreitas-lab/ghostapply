type CamposBuscaveis = { empresa: string; titulo: string; status?: string; modalidade?: string }
type FiltrosCandidaturas = { status?: string; modalidade?: string }

function normalizar(texto: string): string {
  return texto.trim().toLowerCase().normalize('NFD').replace(/\p{M}/gu, '')
}

export function filtrarCandidaturas<T extends CamposBuscaveis>(
  itens: readonly T[],
  busca: string,
  filtros: FiltrosCandidaturas = {},
): T[] {
  const termo = normalizar(busca)
  return itens.filter((item) =>
    (normalizar(item.empresa).includes(termo) || normalizar(item.titulo).includes(termo))
      && (!filtros.status || item.status === filtros.status)
      && (!filtros.modalidade || item.modalidade === filtros.modalidade),
  )
}
