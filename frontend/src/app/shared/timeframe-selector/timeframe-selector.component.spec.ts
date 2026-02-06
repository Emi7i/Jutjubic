import { ComponentFixture, TestBed } from '@angular/core/testing';

import { TimeframeSelectorComponent } from './timeframe-selector.component';

describe('TimeframeSelectorComponent', () => {
  let component: TimeframeSelectorComponent;
  let fixture: ComponentFixture<TimeframeSelectorComponent>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      declarations: [TimeframeSelectorComponent]
    });
    fixture = TestBed.createComponent(TimeframeSelectorComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
