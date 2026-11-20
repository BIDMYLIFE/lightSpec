import Swal from 'sweetalert2'

export function showSuccess(title: string, text?: string) {
  return Swal.fire({ icon: 'success', title, text, confirmButtonText: '確定' })
}

export function showError(title: string, text?: string) {
  return Swal.fire({ icon: 'error', title, text, confirmButtonText: '知道了' })
}
