import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { TranslateModule } from '@ngx-translate/core';
import { FloatingMenuButtonComponent } from './floating-menu-button.component';

describe('FloatingMenuButtonComponent', () => {
  let originalTouchPoints: PropertyDescriptor | undefined;

  beforeAll(() => {
    originalTouchPoints = Object.getOwnPropertyDescriptor(Navigator.prototype, 'maxTouchPoints');
  });

  afterEach(() => {
    // Restablecer al valor real del entorno para no contaminar otros specs.
    if (originalTouchPoints) {
      Object.defineProperty(Navigator.prototype, 'maxTouchPoints', originalTouchPoints);
    }
  });

  /** Sobrescribe navigator.maxTouchPoints antes de instanciar el componente. */
  function setMaxTouchPoints(value: number): void {
    Object.defineProperty(Navigator.prototype, 'maxTouchPoints', {
      configurable: true,
      get: () => value,
    });
  }

  function createComponent(): ComponentFixture<FloatingMenuButtonComponent> {
    TestBed.configureTestingModule({ imports: [FloatingMenuButtonComponent, TranslateModule.forRoot()] });
    const fixture = TestBed.createComponent(FloatingMenuButtonComponent);
    fixture.detectChanges();
    return fixture;
  }

  describe('when the device supports touch', () => {
    beforeEach(() => setMaxTouchPoints(5));

    it('renders the bubble button', () => {
      const fixture = createComponent();
      const button = fixture.debugElement.query(By.css('button.bubble-menu-btn'));
      expect(button).withContext('button should be rendered on touch devices').not.toBeNull();
    });

    it('renders three menu lines inside the icon', () => {
      const fixture = createComponent();
      const lines = fixture.debugElement.queryAll(By.css('.bubble-line'));
      expect(lines.length).toBe(3);
    });

    it('emits menuToggle when clicked', () => {
      const fixture = createComponent();
      const component = fixture.componentInstance;
      let emitted = 0;
      component.menuToggle.subscribe(() => emitted++);

      const button = fixture.debugElement.query(By.css('button.bubble-menu-btn'));
      button.triggerEventHandler('click', new MouseEvent('click'));

      expect(emitted).toBe(1);
    });

    it('stops click propagation so the canvas does not receive the event', () => {
      const fixture = createComponent();
      const stopSpy = jasmine.createSpy('stopPropagation');
      const fakeEvent = { stopPropagation: stopSpy } as unknown as MouseEvent;

      fixture.componentInstance.onClick(fakeEvent);

      expect(stopSpy).toHaveBeenCalledTimes(1);
    });
  });

  describe('when the device does not support touch', () => {
    beforeEach(() => setMaxTouchPoints(0));

    it('does not render the button (desktop should keep using Escape)', () => {
      const fixture = createComponent();
      const button = fixture.debugElement.query(By.css('button.bubble-menu-btn'));
      expect(button).withContext('button must stay hidden on desktop').toBeNull();
    });

    it('still exposes the menuToggle output for parent template binding', () => {
      const fixture = createComponent();
      expect(fixture.componentInstance.menuToggle).toBeDefined();
    });
  });
});
