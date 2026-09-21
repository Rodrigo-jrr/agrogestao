interface LogoMarkProps {
  size?: number
  className?: string
}

export function LogoMark({ size = 32, className = '' }: LogoMarkProps) {
  return (
    <svg
      width={size}
      height={size}
      viewBox="0 0 40 40"
      fill="none"
      xmlns="http://www.w3.org/2000/svg"
      className={className}
      aria-hidden="true"
    >
      <polygon points="20,2 36,20 4,20" fill="#B5502F" />
      <polygon points="4,20 20,20 20,38" fill="#3F4A34" />
      <polygon points="20,20 36,20 20,38" fill="#C9A24B" />
    </svg>
  )
}

interface LogoProps {
  size?: number
  variant?: 'dark' | 'light'
  className?: string
}

export function Logo({ size = 28, variant = 'dark', className = '' }: LogoProps) {
  const textColor = variant === 'light' ? 'text-terreiro' : 'text-terra'
  return (
    <div className={`flex items-center gap-2 ${className}`}>
      <LogoMark size={size} />
      <span className={`font-serif text-2xl font-semibold ${textColor}`}>
        Agro<span className="text-rust">Rust</span>
      </span>
    </div>
  )
}
