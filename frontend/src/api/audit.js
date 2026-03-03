import request from './axios'

export function getAuditLogs(params) {
  return request({
    url: '/admin/audit-logs',
    method: 'get',
    params
  })
}
