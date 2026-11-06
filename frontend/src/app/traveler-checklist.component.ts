import { Component, Input, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';

export interface NextAction {
  title: string;
  plainLanguage: string;
  internalTerm: string;
  workflexTerm: string;
  owner: string;
  dueDate: string;
  required: boolean;
  documentType: string;
}

@Component({
  selector: 'app-traveler-checklist',
  standalone: true,
  imports: [CommonModule],
  template: `
    <section aria-label="Next actions">
      <div *ngIf="loading">Loading your compliance checklist…</div>
      <div *ngIf="error" role="alert">{{ error }}</div>
      <article class="card" *ngFor="let action of actions">
        <span class="badge">{{ action.workflexTerm }}</span>
        <h2>{{ action.title }}</h2>
        <p>{{ action.plainLanguage }}</p>
        <p><strong>Owner:</strong> {{ action.owner }} · <strong>Due:</strong> {{ action.dueDate }} · <strong>Required:</strong> {{ action.required ? 'Yes' : 'No' }}</p>
      </article>
    </section>
  `
})
export class TravelerChecklistComponent implements OnInit {
  @Input() tripId = '';
  actions: NextAction[] = [];
  loading = true;
  error = '';

  constructor(private http: HttpClient) {}

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading = true;
    this.http.get<NextAction[]>(`/api/v1/trips/${this.tripId}/next-actions`).subscribe({
      next: (data) => {
        this.actions = data;
        this.loading = false;
      },
      error: () => {
        this.error = 'Could not load actions. Please retry.';
        this.loading = false;
      }
    });
  }
}
