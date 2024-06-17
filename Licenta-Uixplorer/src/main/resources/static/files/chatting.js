
    document.addEventListener('DOMContentLoaded', function() {
      setScrollPosition();

      document.querySelectorAll('form').forEach(form => {
          form.addEventListener('submit', saveScrollPosition);
      });

      // Handle form submission with AJAX
      const sendMessageForm = document.getElementById('sendMessageForm');
      sendMessageForm.addEventListener('submit', function(event) {
          event.preventDefault();
          sendMessageAjax();
      });
  });

  function saveScrollPosition() {
      localStorage.setItem('scrollPosition', window.scrollY);
  }

  function setScrollPosition() {
      const scrollPosition = localStorage.getItem('scrollPosition');
      if (scrollPosition) {
          window.scrollTo(0, parseInt(scrollPosition, 10));
          localStorage.removeItem('scrollPosition');
      }
  }

  let chatWindow = document.getElementById('chatWindow');
  let chatFriendName = document.getElementById('chatFriendName');
  let chatBody = document.getElementById('chatBody');
  let friendId = null;


  function openChat(friendItemElement) {

      friendId = friendItemElement.querySelector('input[name="chosenFriendId"]').value;
      chatWindow.style.display = 'block';
      closeFriendsMenu();

      fetch(`/get-friend-details?friendId=${friendId}`)
          .then(response => response.json())
          .then(friend => {
              document.querySelector('.chat-header h5').innerText = friend.name;
              document.querySelector('.chat-header img').src = friend.image;
              loadChatMessages(friendId);
          })
          .catch(error => console.error('Error loading friend details:', error));
  }

  function loadChatMessages(friendId) {
      fetch(`/chat/messages?friendId=${friendId}`)
          .then(response => response.json())
          .then(messages => {
              const messagesMenu = document.getElementById('messagesMenu');
              messagesMenu.innerHTML = '';
              messages.forEach(message => {
                  const messageItem = document.createElement('div');
                  messageItem.className = 'message-item';

                  const messagePic = document.createElement('div');
                  messagePic.className = 'message-pic';

                  const img = document.createElement('img');
                  img.src = message.personImage;
                  messagePic.appendChild(img);

                  const messageText = document.createElement('p');
                  messageText.textContent = message.message;

                  messageItem.appendChild(messagePic);
                  messageItem.appendChild(messageText);
                  messagesMenu.appendChild(messageItem);
              });
              scrollToBottom();
          })
          .catch(error => console.error('Failed to load messages:', error));
  }

  function sendMessageAjax() {
      const formData = new FormData(document.getElementById('sendMessageForm'));
      formData.append('chosenFriendId', friendId);
      const chatmsg = formData.get('chatmsg');

      fetch('/send-message', {
          method: 'POST',
          body: new URLSearchParams(formData),
          headers: {
              'Content-Type': 'application/x-www-form-urlencoded'
          }
      })
      .then(response => response.json())
      .then(message => {
          const messagesMenu = document.getElementById('messagesMenu');
          const messageItem = document.createElement('div');
          messageItem.className = 'message-item';

          const messagePic = document.createElement('div');
          messagePic.className = 'message-pic';

          const img = document.createElement('img');
          img.src = message.personImage;
          messagePic.appendChild(img);

          const messageText = document.createElement('p');
          messageText.textContent = message.message;

          messageItem.appendChild(messagePic);
          messageItem.appendChild(messageText);
          messagesMenu.appendChild(messageItem);

          const chatInput = document.getElementById('chatInput');
          chatInput.value = '';
          chatInput.placeholder = 'Type a message...';
          scrollToBottom();
      })
      .catch(error => console.error('Error sending message:', error));
  }

  function closeChat() {
      friendId = null;
      chatWindow.style.display = 'none';
  }

  function scrollToBottom() {
      const chatBody = document.getElementById('chatBody');
      chatBody.scrollTop = chatBody.scrollHeight;
  }
