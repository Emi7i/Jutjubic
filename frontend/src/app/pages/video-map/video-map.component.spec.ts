import { ComponentFixture, TestBed } from '@angular/core/testing';

import { VideoMapComponent } from './video-map.component';

describe('VideoMapComponent', () => {
  let component: VideoMapComponent;
  let fixture: ComponentFixture<VideoMapComponent>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      declarations: [VideoMapComponent]
    });
    fixture = TestBed.createComponent(VideoMapComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
