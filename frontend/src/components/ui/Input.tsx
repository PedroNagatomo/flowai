import { type InputHTMLAttributes, forwardRef } from 'react';

interface Props extends InputHTMLAttributes<HTMLInputElement> {
  label?: string;
  error?: string;
}

export const Input = forwardRef<HTMLInputElement, Props>(
  ({ label, error, id, className = '', ...rest }, ref) => {
    const inputId = id || rest.name;
    return (
      <div>
        {label && (
          <label htmlFor={inputId} className="label">
            {label}
          </label>
        )}
        <input
          ref={ref}
          id={inputId}
          className={`input ${error ? 'border-danger' : ''} ${className}`}
          {...rest}
        />
        {error && <p className="text-danger text-xs mt-1">{error}</p>}
      </div>
    );
  }
);
Input.displayName = 'Input';