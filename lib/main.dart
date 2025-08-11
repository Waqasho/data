import 'package:flutter/material.dart';
import 'package:webview_flutter/webview_flutter.dart';
import 'package:url_launcher/url_launcher.dart';

void main() {
  runApp(const MyApp());
}

class MyApp extends StatelessWidget {
  const MyApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Unlimited Browser Windows',
      debugShowCheckedModeBanner: false,
      theme: ThemeData(
        colorScheme: ColorScheme.fromSeed(
          seedColor: const Color(0xFF2196F3),
          primary: const Color(0xFF2196F3),
          secondary: const Color(0xFF4CAF50),
        ),
        useMaterial3: true,
      ),
      home: const BrowserHomeScreen(),
    );
  }
}

class BrowserHomeScreen extends StatefulWidget {
  const BrowserHomeScreen({super.key});

  @override
  State<BrowserHomeScreen> createState() => _BrowserHomeScreenState();
}

class _BrowserHomeScreenState extends State<BrowserHomeScreen> {
  final List<BrowserWindow> _windows = [];
  int _currentWindowIndex = 0;

  @override
  void initState() {
    super.initState();
    // Add initial window
    _addNewWindow();
  }

  void _addNewWindow() {
    setState(() {
      _windows.add(BrowserWindow(
        id: DateTime.now().millisecondsSinceEpoch.toString(),
        title: 'New Tab',
        url: 'https://www.google.com',
      ));
      _currentWindowIndex = _windows.length - 1;
    });
  }

  void _removeWindow(int index) {
    if (_windows.length > 1) {
      setState(() {
        _windows.removeAt(index);
        if (_currentWindowIndex >= _windows.length) {
          _currentWindowIndex = _windows.length - 1;
        }
      });
    }
  }

  void _switchWindow(int index) {
    setState(() {
      _currentWindowIndex = index;
    });
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Unlimited Browser Windows'),
        backgroundColor: Theme.of(context).colorScheme.primary,
        foregroundColor: Colors.white,
        actions: [
          IconButton(
            icon: const Icon(Icons.add),
            onPressed: _addNewWindow,
            tooltip: 'New Window',
          ),
        ],
      ),
      body: Column(
        children: [
          // Tab bar for windows
          Container(
            height: 50,
            color: Colors.grey[200],
            child: ListView.builder(
              scrollDirection: Axis.horizontal,
              itemCount: _windows.length,
              itemBuilder: (context, index) {
                final window = _windows[index];
                final isActive = index == _currentWindowIndex;
                
                return Container(
                  margin: const EdgeInsets.all(4),
                  child: Material(
                    color: isActive ? Colors.white : Colors.grey[300],
                    borderRadius: BorderRadius.circular(8),
                    child: InkWell(
                      onTap: () => _switchWindow(index),
                      borderRadius: BorderRadius.circular(8),
                      child: Container(
                        padding: const EdgeInsets.symmetric(horizontal: 12),
                        child: Row(
                          mainAxisSize: MainAxisSize.min,
                          children: [
                            Flexible(
                              child: Text(
                                window.title,
                                style: TextStyle(
                                  color: isActive ? Colors.black : Colors.grey[700],
                                  fontWeight: isActive ? FontWeight.bold : FontWeight.normal,
                                ),
                                overflow: TextOverflow.ellipsis,
                              ),
                            ),
                            if (_windows.length > 1) ...[
                              const SizedBox(width: 8),
                              GestureDetector(
                                onTap: () => _removeWindow(index),
                                child: Icon(
                                  Icons.close,
                                  size: 16,
                                  color: isActive ? Colors.grey[600] : Colors.grey[500],
                                ),
                              ),
                            ],
                          ],
                        ),
                      ),
                    ),
                  ),
                );
              },
            ),
          ),
          // Browser content
          Expanded(
            child: _windows.isEmpty
                ? const Center(child: Text('No windows available'))
                : BrowserWindowWidget(
                    window: _windows[_currentWindowIndex],
                    onTitleChanged: (title) {
                      setState(() {
                        _windows[_currentWindowIndex].title = title;
                      });
                    },
                    onUrlChanged: (url) {
                      setState(() {
                        _windows[_currentWindowIndex].url = url;
                      });
                    },
                  ),
          ),
        ],
      ),
      floatingActionButton: FloatingActionButton(
        onPressed: _addNewWindow,
        backgroundColor: Theme.of(context).colorScheme.primary,
        foregroundColor: Colors.white,
        child: const Icon(Icons.add),
      ),
    );
  }
}

class BrowserWindow {
  final String id;
  String title;
  String url;

  BrowserWindow({
    required this.id,
    required this.title,
    required this.url,
  });
}

class BrowserWindowWidget extends StatefulWidget {
  final BrowserWindow window;
  final Function(String) onTitleChanged;
  final Function(String) onUrlChanged;

  const BrowserWindowWidget({
    super.key,
    required this.window,
    required this.onTitleChanged,
    required this.onUrlChanged,
  });

  @override
  State<BrowserWindowWidget> createState() => _BrowserWindowWidgetState();
}

class _BrowserWindowWidgetState extends State<BrowserWindowWidget> {
  late WebViewController _controller;
  final TextEditingController _urlController = TextEditingController();
  bool _isLoading = false;
  bool _canGoBack = false;
  bool _canGoForward = false;

  @override
  void initState() {
    super.initState();
    _urlController.text = widget.window.url;
    _initializeWebView();
  }

  void _initializeWebView() {
    _controller = WebViewController()
      ..setJavaScriptMode(JavaScriptMode.unrestricted)
      ..setNavigationDelegate(
        NavigationDelegate(
          onProgress: (int progress) {
            // Update loading progress
          },
          onPageStarted: (String url) {
            setState(() {
              _isLoading = true;
            });
          },
          onPageFinished: (String url) {
            setState(() {
              _isLoading = false;
            });
            _updateNavigationState();
            _updateTitle();
          },
          onNavigationRequest: (NavigationRequest request) {
            widget.onUrlChanged(request.url);
            return NavigationDecision.navigate;
          },
        ),
      )
      ..loadRequest(Uri.parse(widget.window.url));
  }

  void _updateNavigationState() {
    _controller.canGoBack().then((canGoBack) {
      if (mounted) {
        setState(() {
          _canGoBack = canGoBack;
        });
      }
    });
    _controller.canGoForward().then((canGoForward) {
      if (mounted) {
        setState(() {
          _canGoForward = canGoForward;
        });
      }
    });
  }

  void _updateTitle() {
    _controller.getTitle().then((title) {
      if (mounted && title != null) {
        widget.onTitleChanged(title);
      }
    });
  }

  void _navigateToUrl() {
    String url = _urlController.text.trim();
    if (!url.startsWith('http://') && !url.startsWith('https://')) {
      url = 'https://$url';
    }
    
    widget.onUrlChanged(url);
    _controller.loadRequest(Uri.parse(url));
  }

  void _refresh() {
    _controller.reload();
  }

  void _goBack() {
    _controller.goBack();
  }

  void _goForward() {
    _controller.goForward();
  }

  @override
  Widget build(BuildContext context) {
    return Column(
      children: [
        // Navigation bar
        Container(
          padding: const EdgeInsets.all(8),
          color: Colors.grey[100],
          child: Row(
            children: [
              IconButton(
                icon: const Icon(Icons.arrow_back),
                onPressed: _canGoBack ? _goBack : null,
                tooltip: 'Go Back',
              ),
              IconButton(
                icon: const Icon(Icons.arrow_forward),
                onPressed: _canGoForward ? _goForward : null,
                tooltip: 'Go Forward',
              ),
              IconButton(
                icon: const Icon(Icons.refresh),
                onPressed: _refresh,
                tooltip: 'Refresh',
              ),
              const SizedBox(width: 8),
              Expanded(
                child: TextField(
                  controller: _urlController,
                  decoration: InputDecoration(
                    hintText: 'Enter URL',
                    border: OutlineInputBorder(
                      borderRadius: BorderRadius.circular(8),
                    ),
                    contentPadding: const EdgeInsets.symmetric(
                      horizontal: 12,
                      vertical: 8,
                    ),
                    suffixIcon: IconButton(
                      icon: const Icon(Icons.search),
                      onPressed: _navigateToUrl,
                    ),
                  ),
                  onSubmitted: (_) => _navigateToUrl(),
                ),
              ),
              const SizedBox(width: 8),
              if (_isLoading)
                const SizedBox(
                  width: 20,
                  height: 20,
                  child: CircularProgressIndicator(strokeWidth: 2),
                ),
            ],
          ),
        ),
        // WebView content
        Expanded(
          child: WebViewWidget(controller: _controller),
        ),
      ],
    );
  }

  @override
  void dispose() {
    _urlController.dispose();
    super.dispose();
  }
}
