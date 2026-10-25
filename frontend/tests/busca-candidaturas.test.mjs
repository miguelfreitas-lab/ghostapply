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
