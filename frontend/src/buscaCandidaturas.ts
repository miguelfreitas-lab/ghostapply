type CamposBuscaveis = { empresa: string; titulo: string }

function normalizar(texto: string): string {
  return texto.trim().toLowerCase().normalize('NFD').replace(/\p{M}/gu, '')
}

export function filtrarCandidaturas<T extends CamposBuscaveis>(itens: readonly T[], busca: string): T[] {
  const termo = normalizar(busca)
  return itens.filter((item) =>
    normalizar(item.empresa).includes(termo) || normalizar(item.titulo).includes(termo),
  )
}
