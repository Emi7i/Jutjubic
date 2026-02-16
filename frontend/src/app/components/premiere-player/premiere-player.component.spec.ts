import { ComponentFixture, TestBed } from '@angular/core/testing';

import { PremierePlayerComponent } from './premiere-player.component';

describe('PremierePlayerComponent', () => {
  let component: PremierePlayerComponent;
  let fixture: ComponentFixture<PremierePlayerComponent>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      declarations: [PremierePlayerComponent]
    });
    fixture = TestBed.createComponent(PremierePlayerComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
