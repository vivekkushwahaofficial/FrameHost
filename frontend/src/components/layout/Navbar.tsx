function Navbar() {
  return (
    <header className="border-b border-gray-200 bg-white">
      <nav
        className="mx-auto flex max-w-7xl items-center justify-between px-6 py-4"
        aria-label="Main navigation"
      >
        {/* FrameHost branding */}
        <a
          href="/"
          className="text-2xl font-bold tracking-tight text-gray-900"
          aria-label="FrameHost home"
        >
          FrameHost<span className="text-blue-600">.</span>
        </a>

        {/* Desktop navigation links */}
        <div className="hidden items-center gap-8 md:flex">
          <a
            href="#features"
            className="text-sm font-medium text-gray-600 transition-colors hover:text-blue-600"
          >
            Features
          </a>

          <a
            href="#how-it-works"
            className="text-sm font-medium text-gray-600 transition-colors hover:text-blue-600"
          >
            How it works
          </a>

          <a
            href="#device-roles"
            className="text-sm font-medium text-gray-600 transition-colors hover:text-blue-600"
          >
            Device roles
          </a>
        </div>

        {/* Get started action */}
        <a
          href="#get-started"
          className="rounded-lg bg-blue-600 px-4 py-2 text-sm font-semibold text-white transition-colors hover:bg-blue-700 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-blue-600"
        >
          Get started
        </a>
      </nav>
    </header>
  );
}

export default Navbar;
