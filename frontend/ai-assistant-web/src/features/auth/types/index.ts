export interface LoginFormState {
  username: string
  pwd: string
  captchaId: string
  code: string
  role: 'USER' | 'ADMIN'
}

export interface RegisterFormState {
  username: string
  pwd: string
  captchaId: string
  code: string
}
