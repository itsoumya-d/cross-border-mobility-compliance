import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClientModule } from '@angular/common/http';
import { TravelerChecklistComponent } from './traveler-checklist.component';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule, HttpClientModule, TravelerChecklistComponent],
  template: `
    <main style="max-width:880px;margin:0 auto;padding:24px">
      <h1>Cross-border mobility — your next actions</h1>
      <p>Every required document, deadline and owner in one place.</p>
      <app-traveler-checklist [tripId]="'demo-trip-1'"></app-traveler-checklist>
    </main>
  `
})
export class AppComponent {
  title = 'Cross-Border Mobility Compliance';
}
