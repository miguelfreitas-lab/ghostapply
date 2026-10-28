export type OrdenacaoCandidaturas = 'recentes' | 'antigas'
type CamposOrdenaveis = { id: number; criadaEm?: string | null }

export function ordenarCandidaturas<T extends CamposOrdenaveis>(
  itens: readonly T[],
  ordem: OrdenacaoCandidaturas = 'recentes',
): T[] {
  const direcao = ordem === 'recentes' ? -1 : 1
  return [...itens].sort((a, b) => {
    const dataA = a.criadaEm ? Date.parse(a.criadaEm) : NaN
    const dataB = b.criadaEm ? Date.parse(b.criadaEm) : NaN
    const validaA = Number.isFinite(dataA)
    const validaB = Number.isFinite(dataB)

    // Datas desconhecidas permanecem no final, com ordenação estável entre elas.
    if (!validaA && !validaB) return 0
    if (!validaA) return 1
    if (!validaB) return -1
    return direcao * (dataA - dataB || a.id - b.id)
  })
}
