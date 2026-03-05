import request from './axios'

export function getClubTransactions(clubId) {
  return request({
    url: `/finance/clubs/${clubId}/transactions`,
    method: 'get'
  })
}

export function createTransaction(data) {
  return request({
    url: '/finance/transactions',
    method: 'post',
    data
  })
}

export function getClubBalance(clubId) {
  return request({
    url: `/finance/clubs/${clubId}/balance`,
    method: 'get'
  })
}

export function approveTransaction(id) {
  return request({
    url: `/finance/transactions/${id}/approve`,
    method: 'post'
  })
}

export function rejectTransaction(id) {
  return request({
    url: `/finance/transactions/${id}/reject`,
    method: 'post'
  })
}
