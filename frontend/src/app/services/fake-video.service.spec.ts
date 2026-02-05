import { TestBed } from '@angular/core/testing';

import { FakeVideoService } from './fake-video.service';

describe('FakeVideoService', () => {
  let service: FakeVideoService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(FakeVideoService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
