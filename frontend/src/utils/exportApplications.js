import { formatEnumLabel } from './formatEnumLabel'

export const EXPORT_FORMATS = [
  { value: 'csv', label: 'CSV (.csv)' },
  { value: 'xlsx', label: 'Excel (.xlsx)' },
  { value: 'json', label: 'JSON (.json)' },
  { value: 'pdf', label: 'PDF (.pdf)' },
]

// Shared column definition for the tabular formats (CSV, Excel, PDF).
// JSON exports the raw API objects instead, so nothing is lost there.
const COLUMNS = [
  { header: 'Company', width: 24, get: app => app.companyName },
  { header: 'Job Title', width: 28, get: app => app.jobTitle },
  { header: 'Status', width: 14, get: app => formatEnumLabel(app.status) },
  { header: 'Source', width: 18, get: app => formatEnumLabel(app.source) },
  { header: 'Applied Date', width: 14, get: app => app.appliedDate },
  { header: 'Job URL', width: 40, get: app => app.jobUrl },
  { header: 'Notes', width: 50, get: app => app.notes },
  { header: 'Created At', width: 20, get: app => formatTimestamp(app.createdAt) },
  { header: 'Updated At', width: 20, get: app => formatTimestamp(app.updatedAt) },
]

function formatTimestamp(value) {
  // "2026-06-01T14:03:22.123" -> "2026-06-01 14:03"
  return value ? value.slice(0, 16).replace('T', ' ') : ''
}

function toRows(applications) {
  return applications.map(app => COLUMNS.map(col => col.get(app) ?? ''))
}

function buildFileName(extension) {
  const today = new Date().toISOString().slice(0, 10)
  return `applications-${today}.${extension}`
}

function downloadBlob(blob, fileName) {
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = fileName
  document.body.appendChild(link)
  link.click()
  link.remove()
  // Revoke on the next tick so the browser has started the download first.
  setTimeout(() => URL.revokeObjectURL(url), 0)
}

function escapeCsvValue(value) {
  let text = String(value)
  // Spreadsheet apps execute cells starting with these characters as formulas
  // (CSV injection), so prefix them with a quote to force plain text.
  if (/^[=+\-@]/.test(text)) text = `'${text}`
  if (/[",\r\n]/.test(text)) text = `"${text.replace(/"/g, '""')}"`
  return text
}

function exportCsv(applications) {
  const lines = [COLUMNS.map(col => col.header), ...toRows(applications)]
    .map(row => row.map(escapeCsvValue).join(','))
  // The BOM makes Excel open the file as UTF-8 instead of guessing the encoding.
  const blob = new Blob(['﻿' + lines.join('\r\n')], { type: 'text/csv;charset=utf-8' })
  downloadBlob(blob, buildFileName('csv'))
}

function exportJson(applications) {
  const blob = new Blob([JSON.stringify(applications, null, 2)], { type: 'application/json' })
  downloadBlob(blob, buildFileName('json'))
}

// The Excel and PDF libraries are loaded on demand with dynamic import(),
// so they are only downloaded when someone actually exports in that format.
async function exportXlsx(applications) {
  const { default: writeExcelFile } = await import('write-excel-file/browser')
  const headerRow = COLUMNS.map(col => ({ value: col.header, fontWeight: 'bold' }))
  await writeExcelFile([headerRow, ...toRows(applications)], {
    sheet: 'Applications',
    columns: COLUMNS.map(col => ({ width: col.width })),
    stickyRowsCount: 1,
  }).toFile(buildFileName('xlsx'))
}

async function exportPdf(applications) {
  const { jsPDF } = await import('jspdf')
  const { autoTable } = await import('jspdf-autotable')
  const doc = new jsPDF({ orientation: 'landscape' })
  doc.setFontSize(14)
  doc.text('My Applications', 14, 15)
  autoTable(doc, {
    startY: 20,
    head: [COLUMNS.map(col => col.header)],
    body: toRows(applications),
    styles: { fontSize: 7, overflow: 'linebreak' },
    headStyles: { fillColor: [37, 99, 235] },
  })
  doc.save(buildFileName('pdf'))
}

export async function exportApplications(applications, format) {
  switch (format) {
    case 'csv': return exportCsv(applications)
    case 'xlsx': return exportXlsx(applications)
    case 'json': return exportJson(applications)
    case 'pdf': return exportPdf(applications)
    default: throw new Error(`Unsupported export format: ${format}`)
  }
}
