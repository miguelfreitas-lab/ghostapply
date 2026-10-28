import assert from 'node:assert/strict'
import test from 'node:test'
import { ordenarCandidaturas } from '../src/ordenarCandidaturas.ts'
import { filtrarCandidaturas } from '../src/buscaCandidaturas.ts'

const itens = Object.freeze([
  Object.freeze({ id: 1, criadaEm: '2026-10-20T10:00:00Z' }),
  Object.freeze({ id: 2, criadaEm: '2026-10-22T09:00:00Z' }),
  Object.freeze({ id: 3, criadaEm: '2026-10-22T09:00:01Z' }),
])
const ids = (lista, ordem) => ordenarCandidaturas(lista, ordem).map((item) => item.id)

test('ordena por data e horário completos nas duas direções e usa recentes como padrão', () => {
  assert.deepEqual(ids(itens), [3, 2, 1])
  assert.deepEqual(ids(itens, 'recentes'), [3, 2, 1])
  assert.deepEqual(ids(itens, 'antigas'), [1, 2, 3])
})

test('desempata datas iguais e fusos equivalentes por ID na direção selecionada', () => {
  const empates = [
    { id: 8, criadaEm: '2026-10-22T09:00:00-03:00' },
    { id: 2, criadaEm: '2026-10-22T12:00:00Z' },
    { id: 5, criadaEm: '2026-10-22T12:00:00Z' },
  ]
  assert.deepEqual(ids(empates, 'recentes'), [8, 5, 2])
  assert.deepEqual(ids(empates, 'antigas'), [2, 5, 8])
})

test('compara instantes e não a representação textual dos fusos', () => {
  const fusos = [
    { id: 1, criadaEm: '2026-10-22T10:00:00+03:00' },
    { id: 2, criadaEm: '2026-10-22T08:00:00Z' },
  ]
  assert.deepEqual(ids(fusos, 'recentes'), [2, 1])
  assert.deepEqual(ids(fusos, 'antigas'), [1, 2])
})

test('datas ausentes e inválidas ficam no final, preservando sua ordem nas duas direções', () => {
  const incompletos = [
    { id: 9 }, itens[1], { id: 7, criadaEm: 'inválida' },
    { id: 3, criadaEm: null }, itens[0], { id: 1, criadaEm: '' },
  ]
  assert.deepEqual(ids(incompletos, 'recentes'), [2, 1, 9, 7, 3, 1])
  assert.deepEqual(ids(incompletos, 'antigas'), [1, 2, 9, 7, 3, 1])
})

test('aceita lista vazia e listas com um elemento válido ou sem data', () => {
  for (const ordem of ['recentes', 'antigas']) {
    assert.deepEqual(ordenarCandidaturas([], ordem), [])
    assert.deepEqual(ordenarCandidaturas([itens[0]], ordem), [itens[0]])
    assert.deepEqual(ordenarCandidaturas([{ id: 1 }], ordem), [{ id: 1 }])
  }
})

test('não altera o array original nem os objetos e sempre retorna uma cópia', () => {
  const antes = structuredClone(itens)
  for (const ordem of ['recentes', 'antigas']) {
    const resultado = ordenarCandidaturas(itens, ordem)
    assert.notStrictEqual(resultado, itens)
    assert.deepEqual(itens, antes)
    assert.ok(resultado.every((item) => itens.includes(item)))
  }
})

test('ordena após combinar busca, status e modalidade, mantendo o total original', () => {
  const lista = [
    { ...itens[0], empresa: 'São José', titulo: 'Java', status: 'SALVA', modalidade: 'HIBRIDO' },
    { ...itens[2], empresa: 'Outra', titulo: 'Java Júnior', status: 'SALVA', modalidade: 'HIBRIDO' },
    { ...itens[1], empresa: 'Outra', titulo: 'Java', status: 'OFERTA', modalidade: 'REMOTO' },
  ]
  const antes = structuredClone(lista)
  const filtrados = filtrarCandidaturas(lista, ' JAVA ', { status: 'SALVA', modalidade: 'HIBRIDO' })
  assert.deepEqual(ids(filtrados, 'recentes'), [3, 1])
  assert.deepEqual(ids(filtrados, 'antigas'), [1, 3])
  assert.equal(filtrados.length, 2)
  assert.equal(lista.length, 3)
  assert.equal(lista.filter((item) => item.status === 'OFERTA').length, 1)
  assert.deepEqual(lista, antes)
})

test('reaplica a mesma ordenação após cadastro, edição, exclusão e atualização', () => {
  for (const ordem of ['recentes', 'antigas']) {
    const nova = { id: 4, criadaEm: '2026-10-23T12:00:00Z', empresa: 'Nova' }
    const cadastrados = [nova, ...itens]
    const esperados = ordem === 'recentes' ? [4, 3, 2, 1] : [1, 2, 3, 4]
    assert.deepEqual(ids(cadastrados, ordem), esperados)
    const editados = cadastrados.map((item) => item.id === 2 ? { ...item, empresa: 'Editada' } : item)
    assert.deepEqual(ids(editados, ordem), esperados)
    assert.deepEqual(ids(editados.filter((item) => item.id !== 3), ordem), esperados.filter((id) => id !== 3))
    assert.deepEqual(ids([...cadastrados].reverse(), ordem), esperados)
  }
})
