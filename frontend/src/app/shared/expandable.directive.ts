import { Directive, ElementRef, Input, Renderer2, HostBinding } from '@angular/core';

@Directive({
  selector: '[appExpandable]'
})
export class ExpandableDirective {

  @Input() transitionDuration = 400; // ms
  @HostBinding('class.open') isOpen = false;

  constructor(private el: ElementRef, private renderer: Renderer2) {
    this.renderer.setStyle(this.el.nativeElement, 'overflow', 'hidden');
    this.renderer.setStyle(this.el.nativeElement, 'height', '0px');
    this.renderer.setStyle(this.el.nativeElement, 'transition', `height ${this.transitionDuration}ms ease`);
  }

  open(targetHeight?: string) {
    this.isOpen = true;
    const height = targetHeight ?? `${this.el.nativeElement.scrollHeight}px`;
    this.renderer.setStyle(this.el.nativeElement, 'height', height);
  }

  close() {
    this.isOpen = false;
    this.renderer.setStyle(this.el.nativeElement, 'height', '0px');
  }

  toggle(targetHeight?: string) {
    if (this.isOpen) {
      this.close();
    } else {
      this.open(targetHeight);
    }
  }
}
