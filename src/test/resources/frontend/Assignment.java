class Assignment {
    public int f(int x, int y) {
        int x1 = (x + 1) * 1000;
        int x2 = y / 12 % (x1 * 90);
        int x3 = x1 = 10;
        return x + x1 - y - x2 - x3;
    }

    public void g(int x, int y) {
        int x1 = x + 3, y1 = y + 2;
    }

    private int a;
    public void k() {
        int x = 10;
        int q = (x += 10);
        q /= 20;
        q -= 20;
        q = (q += 20);
        this.a &= 20;
        this.a |= 20;
        this.a %= 20;
        this.a >>= 20;
        this.a <<= 20;
        this.a >>>= 20;
    }
}