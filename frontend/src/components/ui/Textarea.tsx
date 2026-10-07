import { type TextareaHTMLAttributes, forwardRef } from 'react';

interface Props extends TextareaHTMLAttributes<HTMLTextAreaElement> {
  label?: string;
  error?: string;
}

export const Textarea = forwardRef<HTMLTextAreaElement, Props>(
  ({ label, error, id, className = '', ...rest }, ref) => {
    const inputId = id || rest.name;
    return (
      <div>
        {label && (
          <label htmlFor={inputId} className="label">
            {label}
          </label>
        )}
        <textarea
          ref={ref}
          id={inputId}
          className={`input font-mono text-sm ${error ? 'border-danger' : ''} ${className}`}
          {...rest}
        />
        {error && <p className="text-danger text-xs mt-1">{error}</p>}
      </div>
    );
  }
);
Textarea.displayName = 'Textarea';