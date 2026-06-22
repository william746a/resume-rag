import { CommonModule } from '@angular/common';
import { Component, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MarkdownComponent } from 'ngx-markdown';

const LAST_UPDATED = 'May 2026';

@Component({
  selector: 'app-chat',
  imports: [CommonModule, FormsModule, MarkdownComponent],
  templateUrl: './chat.component.html',
  styleUrl: './chat.component.scss',
})
export class ChatComponent {
  readonly lastUpdated = LAST_UPDATED;

  readonly suggestedQuestions = [
    'What JVM frameworks are in the resume?',
    'Describe a challenging migration they led.',
    'Are remote roles mentioned?',
  ];

  question = '';

  readonly assistantMessage = signal('');

  readonly loading = signal(false);

  readonly error = signal<string | null>(null);

  async send(prefill?: string): Promise<void> {
    const q = (prefill ?? this.question).trim();
    if (!q || this.loading()) {
      return;
    }

    this.error.set(null);
    this.assistantMessage.set('');
    this.loading.set(true);
    if (prefill !== undefined) {
      this.question = q;
    }

    try {
      const res = await fetch('/api/chat', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          Accept: 'text/event-stream',
        },
        body: JSON.stringify({ question: q }),
      });

      if (!res.ok) {
        this.error.set(await readHttpError(res));
        return;
      }

      const body = res.body;
      if (!body) {
        this.error.set('No response body');
        return;
      }

      const reader = body.getReader();
      const decoder = new TextDecoder();
      let carry = '';

      while (true) {
        const { done, value } = await reader.read();
        carry += decoder.decode(value ?? new Uint8Array(), { stream: !done });

        let newlineIndex: number;
        while ((newlineIndex = carry.indexOf('\n')) >= 0) {
          const rawLine = carry.slice(0, newlineIndex);
          carry = carry.slice(newlineIndex + 1);

          const line = rawLine.endsWith('\r') ? rawLine.slice(0, -1) : rawLine;
          if (line.startsWith('data:')) {
            const payload = line.slice('data:'.length).trimStart();
            if (payload.length > 0) {
              this.assistantMessage.update((current) => current + payload);
            }
          }
        }

        if (done) {
          break;
        }
      }

      const tail = carry.trimEnd();
      if (tail.length > 0 && tail.startsWith('data:')) {
        const payload = tail.slice('data:'.length).trimStart();
        if (payload.length > 0) {
          this.assistantMessage.update((current) => current + payload);
        }
      }
    } catch (e) {
      this.error.set(e instanceof Error ? e.message : 'Request failed');
    } finally {
      this.loading.set(false);
    }
  }
}

async function readHttpError(res: Response): Promise<string> {
  const text = await res.text();
  const trimmed = text.trim();
  return trimmed.length > 0 ? trimmed : `HTTP ${res.status}`;
}
