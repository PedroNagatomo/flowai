import { render, screen, fireEvent } from '@testing-library/react';
import { describe, it, expect } from 'vitest';
import { Input } from '../Input';

describe('Input', () => {
  it('renders label and input', () => {
    render(<Input label="Email" name="email" />);
    expect(screen.getByLabelText('Email')).toBeInTheDocument();
  });

  it('shows error message', () => {
    render(<Input label="Email" name="email" error="Email inválido" />);
    expect(screen.getByText('Email inválido')).toBeInTheDocument();
  });

  it('accepts typing', () => {
    render(<Input label="Email" name="email" />);
    const input = screen.getByLabelText('Email') as HTMLInputElement;
    fireEvent.change(input, { target: { value: 'test@test.com' } });
    expect(input.value).toBe('test@test.com');
  });
});