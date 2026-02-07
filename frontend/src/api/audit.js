import request from './axios'

export function getAuditLogs(params) {
  return request({
    url: '/api/admin/audit-logs',
    method: 'get',
    params
  })
}
