$ = jQuery

entityMap = {
  "&": "&amp;",
  "<": "&lt;",
  ">": "&gt;",
  '"': '&quot;',
  "'": '&#39;',
  "/": '&#x2F;'
}
escapeHtml = (str) -> str.replace(/[&<>"'\/]/g, (s) -> entityMap[s])

window.confirmDialog = (options) ->
  settings = _.extend window.confirmDialog.defaults, options
  dialog = $ """
             <div>
               <div class='modal-head'><h2>#{escapeHtml(settings.title)}</h2></div>
               <div class='modal-body'>#{escapeHtml(settings.html)}</div>
               <div class='modal-foot'>
                 <button data-confirm='yes'>#{settings.yesLabel}</button>
                 <a data-confirm='no' class='action'>#{settings.noLabel}</a>
               </div>
             </div>
             """

  $('[data-confirm=yes]', dialog).on 'click', ->
    dialog.dialog 'close'
    settings.yesHandler()
    settings.always()
  $('[data-confirm=no]', dialog).on 'click', ->
    dialog.dialog 'close'
    settings.noHandler()
    settings.always()

  dialog.dialog
    modal: true
    minHeight: null
    width: 540


window.confirmDialog.defaults =
  title: 'Confirmation'
  html: ''
  yesLabel: 'Yes'
  noLabel: 'Cancel'
  yesHandler: ->
  noHandler: ->
  always: ->
