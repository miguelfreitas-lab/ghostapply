import assert from 'node:assert/strict'
import test from 'node:test'
import { filtrarCandidaturas } from '../src/buscaCandidaturas.ts'

const itens = Object.freeze([
  Object.freeze({ id: 1, empresa: 'São José Tecnologia', titulo: 'Desenvolvedor Java Júnior', status: 'ENTREVISTA' }),
  Object.freeze({ id: 2, empresa: 'Huawei', titulo: 'Engenheiro Cloud', status: 'SALVA' }),
  Object.freeze({ id: 3, empresa: 'Cloud Brasil', titulo: 'Analista de dados', status: 'OFERTA' }),
])

test('busca parcial por empresa ou título preserva a ordem', () => {
  assert.deepEqual(filtrarCandidaturas(itens, 'hua'), [itens[1]])
  assert.deepEqual(filtrarCandidaturas(itens, 'cloud'), [itens[1], itens[2]])
})

test('ignora caixa, acentos e espaços nas extremidades', () => {
  for (const termo of ['  SÃO JOSÉ  ', 'sao jose', 'JUNIOR', '  júnior ', 'ju\u0301nior']) {
    assert.deepEqual(filtrarCandidaturas(itens, termo), [itens[0]])
  }
})

test('termo vazio ou somente espaços mostra todos os itens', () => {
  for (const termo of ['', '   ', '\t\n']) {
    assert.deepEqual(filtrarCandidaturas(itens, termo), itens)
  }
})

test('busca sem correspondência e lista vazia retornam lista vazia', () => {
  assert.deepEqual(filtrarCandidaturas(itens, 'inexistente'), [])
  assert.deepEqual(filtrarCandidaturas([], 'cloud'), [])
  assert.deepEqual(filtrarCandidaturas([], ''), [])
})

test('não modifica a lista original nem os objetos usados pelos indicadores', () => {
  const antes = structuredClone(itens)
  const resultado = filtrarCandidaturas(itens, 'java')
  assert.deepEqual(itens, antes)
  assert.notStrictEqual(resultado, itens)
  assert.strictEqual(resultado[0], itens[0])
  assert.equal(itens.length, 3)
  assert.equal(itens.filter((item) => item.status === 'OFERTA').length, 1)
})

test('reaplica o mesmo termo após cadastro, edição, exclusão e atualização da lista', () => {
  const termo = 'cloud'
  const nova = { id: 4, empresa: 'Nova empresa', titulo: 'Cloud Júnior', status: 'SALVA' }
  const cadastrados = [nova, ...itens]
  assert.deepEqual(filtrarCandidaturas(cadastrados, termo).map((item) => item.id), [4, 2, 3])

  const editados = cadastrados.map((item) => item.id === 2 ? { ...item, titulo: 'Engenheiro Java' } : item)
  assert.deepEqual(filtrarCandidaturas(editados, termo).map((item) => item.id), [4, 3])

  const excluidos = editados.filter((item) => item.id !== 4)
  assert.deepEqual(filtrarCandidaturas(excluidos, termo).map((item) => item.id), [3])
  assert.deepEqual(filtrarCandidaturas([itens[1]], termo).map((item) => item.id), [2])
  assert.deepEqual(filtrarCandidaturas(itens, ''), itens)
})

const itensComFiltros = Object.freeze([
  Object.freeze({ id: 1, empresa: 'São José', titulo: 'Java Júnior', status: 'SALVA', modalidade: 'REMOTO' }),
  Object.freeze({ id: 2, empresa: 'Cloud Brasil', titulo: 'Analista', status: 'INSCRITO', modalidade: 'HIBRIDO' }),
  Object.freeze({ id: 3, empresa: 'Huawei', titulo: 'Cloud Júnior', status: 'ENTREVISTA', modalidade: 'PRESENCIAL' }),
  Object.freeze({ id: 4, empresa: 'Outra empresa', titulo: 'Cloud Sênior', status: 'OFERTA', modalidade: 'HIBRIDO' }),
  Object.freeze({ id: 5, empresa: 'Empresa Java', titulo: 'Analista', status: 'REJEITADO', modalidade: 'REMOTO' }),
  Object.freeze({ id: 6, empresa: 'Tecnologia', titulo: 'Java Pleno', status: 'ENTREVISTA', modalidade: 'HIBRIDO' }),
])

function ids(busca, filtros, lista = itensComFiltros) {
  return filtrarCandidaturas(lista, busca, filtros).map((item) => item.id)
}

test('filtra isoladamente por cada status canônico', () => {
  for (const [status, esperados] of [
    ['SALVA', [1]], ['INSCRITO', [2]], ['ENTREVISTA', [3, 6]], ['OFERTA', [4]], ['REJEITADO', [5]],
  ]) {
    assert.deepEqual(ids('', { status }), esperados)
  }
})

test('filtra isoladamente por cada modalidade canônica', () => {
  for (const [modalidade, esperados] of [
    ['REMOTO', [1, 5]], ['HIBRIDO', [2, 4, 6]], ['PRESENCIAL', [3]],
  ]) {
    assert.deepEqual(ids('', { modalidade }), esperados)
  }
})

test('combina texto com status ou modalidade e combina os dois seletores', () => {
  assert.deepEqual(ids('cloud', { status: 'OFERTA' }), [4])
  assert.deepEqual(ids('cloud', { modalidade: 'HIBRIDO' }), [2, 4])
  assert.deepEqual(ids('', { status: 'ENTREVISTA', modalidade: 'PRESENCIAL' }), [3])
})

test('aplica E entre os três critérios mantendo a normalização textual', () => {
  assert.deepEqual(ids('  CLÓUD  ', { status: 'OFERTA', modalidade: 'HIBRIDO' }), [4])
  assert.deepEqual(ids('java', { status: 'OFERTA', modalidade: 'HIBRIDO' }), [])
  assert.deepEqual(ids('cloud', { status: 'OFERTA', modalidade: 'REMOTO' }), [])
  assert.deepEqual(ids('cloud', { status: 'REJEITADO', modalidade: 'HIBRIDO' }), [])
})

test('Todos e Todas removem apenas as respectivas restrições', () => {
  assert.deepEqual(ids('cloud', { status: '', modalidade: 'HIBRIDO' }), [2, 4])
  assert.deepEqual(ids('cloud', { status: 'OFERTA', modalidade: '' }), [4])
  assert.deepEqual(ids('cloud', { status: '', modalidade: '' }), [2, 3, 4])
  assert.deepEqual(filtrarCandidaturas(itensComFiltros, '', {}), itensComFiltros)
  assert.deepEqual(filtrarCandidaturas(itensComFiltros, '', { status: '', modalidade: '' }), itensComFiltros)
})

test('os critérios de enum usam valores exatos, sem rótulos acentuados', () => {
  assert.deepEqual(ids('', { modalidade: 'HÍBRIDO' }), [])
  assert.deepEqual(ids('', { modalidade: 'Híbrido' }), [])
  assert.deepEqual(ids('', { status: 'Inscrito' }), [])
  assert.deepEqual(ids('', { status: 'Todos', modalidade: 'Todas' }), [])
  assert.deepEqual(filtrarCandidaturas([], '', { status: 'SALVA', modalidade: 'HIBRIDO' }), [])
})

test('filtros combinados preservam ordem, referências, lista e critérios originais', () => {
  const antes = structuredClone(itensComFiltros)
  const filtros = Object.freeze({ status: '', modalidade: 'HIBRIDO' })
  const resultado = filtrarCandidaturas(itensComFiltros, 'cloud', filtros)
  assert.deepEqual(resultado, [itensComFiltros[1], itensComFiltros[3]])
  assert.strictEqual(resultado[0], itensComFiltros[1])
  assert.strictEqual(resultado[1], itensComFiltros[3])
  assert.deepEqual(itensComFiltros, antes)
  assert.deepEqual(filtros, { status: '', modalidade: 'HIBRIDO' })
  assert.equal(itensComFiltros.length, 6)
  assert.equal(itensComFiltros.filter((item) => item.status === 'ENTREVISTA').length, 2)
})

test('reaplica os mesmos filtros após cadastro, edição, exclusão e atualização', () => {
  const filtros = Object.freeze({ status: 'ENTREVISTA', modalidade: 'HIBRIDO' })
  assert.deepEqual(ids('java', filtros), [6])
  const nova = { id: 7, empresa: 'Nova', titulo: 'Java', status: 'ENTREVISTA', modalidade: 'HIBRIDO' }
  const cadastrados = [nova, ...itensComFiltros]
  assert.deepEqual(ids('java', filtros, cadastrados), [7, 6])
  const editados = cadastrados.map((item) => item.id === 6 ? { ...item, status: 'SALVA' } : item)
  assert.deepEqual(ids('java', filtros, editados), [7])
  assert.deepEqual(ids('java', filtros, editados.filter((item) => item.id !== 7)), [])
  assert.deepEqual(ids('java', filtros, [itensComFiltros[5], nova]), [6, 7])
})
