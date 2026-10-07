import { render, screen, fireEvent } from '@testing-library/react';
import { describe, it, expect, vi } from 'vitest';
import { Button } from '../Button';

describe('Button', () => {
  it('renders children', () => {
    render(<Button>Click me</Button>);
    expect(screen.getByText('Click me')).toBeInTheDocument();
  });

  it('calls onClick when clicked', () => {
    const onClick = vi.fn();
    render(<Button onClick={onClick}>Click</Button>);
    fireEvent.click(screen.getByText('Click'));
    expect(onClick).toHaveBeenCalledTimes(1);
  });

  it('shows loading state', () => {
    render(<Button loading>Salvar</Button>);
    expect(screen.getByText('Carregando...')).toBeInTheDocument();
  });

  it('is disabled when loading', () => {
    const onClick = vi.fn();
    render(<Button loading onClick={onClick}>Salvar</Button>);
    fireEvent.click(screen.getByText('Carregando...'));
    expect(onClick).not.toHaveBeenCalled();
  });
});