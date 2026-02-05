import { Component, EventEmitter, Output } from '@angular/core';

export interface DateInterval {
  from: Date | null;
  to: Date | null;
}

@Component({
  selector: 'app-timeframe-selector',
  templateUrl: './timeframe-selector.component.html',
  styleUrls: ['./timeframe-selector.component.css']
})
export class TimeframeSelectorComponent {

  @Output() intervalChange = new EventEmitter<DateInterval>();

  selected = 'all';

  customFrom?: string;
  customTo?: string;

  onSelectionChange(): void {
    const now = new Date();

    switch (this.selected) {
      case 'all':
        this.emit(null, null);
        break;

      case 'lastMonth': {
        const from = new Date(now);
        from.setMonth(from.getMonth() - 1);
        this.emit(from, now);
        break;
      }

      case 'lastYear': {
        const from = new Date(now);
        from.setFullYear(from.getFullYear() - 1);
        this.emit(from, now);
        break;
      }

      case 'thisYear': {
        const from = new Date(now.getFullYear(), 0, 1);
        this.emit(from, now);
        break;
      }

      case 'custom':
        this.emitCustom();
        break;
    }
  }

  emitCustom(): void {
    if (!this.customFrom || !this.customTo) return;

    const from = new Date(this.customFrom);
    const to = new Date(this.customTo);

    this.emit(from, to);
  }

  private emit(from: Date | null, to: Date | null): void {
    this.intervalChange.emit({ from, to });
  }
}
